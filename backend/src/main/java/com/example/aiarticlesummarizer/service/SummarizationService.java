package com.example.aiarticlesummarizer.service;

import com.example.aiarticlesummarizer.api.dto.SummarizeRequest;
import com.example.aiarticlesummarizer.api.dto.SummarizeResponse;
import com.example.aiarticlesummarizer.model.Summary;
import com.example.aiarticlesummarizer.repository.SummaryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class SummarizationService {

    private static final Logger logger = LoggerFactory.getLogger(SummarizationService.class);

    private final ChatModel chatModel;
    private final UrlFetchingService urlFetchingService;
    private final SummaryRepository summaryRepository;
    private final MeterRegistry meterRegistry;
    private final DemoModeService demoModeService;

    public SummarizationService(ChatModel chatModel,
                                UrlFetchingService urlFetchingService,
                                SummaryRepository summaryRepository,
                                MeterRegistry meterRegistry,
                                DemoModeService demoModeService) {
        this.chatModel = chatModel;
        this.urlFetchingService = urlFetchingService;
        this.summaryRepository = summaryRepository;
        this.meterRegistry = meterRegistry;
        this.demoModeService = demoModeService;
    }

    @Transactional
    public SummarizeResponse summarize(SummarizeRequest request) throws IOException {
        if (demoModeService.isDemoMode()) {
            return summarizeDemo(request);
        }
        // Safely validate and normalize targetLength to prevent prompt injection
        String requestedTargetLength = request.getTargetLength();
        final String targetLength = com.example.aiarticlesummarizer.api.dto.TargetLength
                .fromString(requestedTargetLength)
                .getValue();

        // Handle URL fetching if URL is provided
        String content;
        String sourceUrl = request.getUrl();
        String articleTitle = null;

        final String source = (sourceUrl != null && !sourceUrl.isBlank()) ? "url" : "text";
        Timer.Sample sample = Timer.start(meterRegistry);

        if (sourceUrl != null && !sourceUrl.isBlank()) {
            // Fetch content from URL
            content = urlFetchingService.fetchArticleContent(sourceUrl);
            articleTitle = urlFetchingService.fetchArticleTitle(sourceUrl);
        } else {
            // Use provided content
            content = Objects.requireNonNull(request.getContent(), "content must not be null");
        }

        // Truncate content if too long (to prevent token limits)
        if (content.length() > 8000) {
            content = content.substring(0, 8000) + "... [truncated]";
        }

        long start = System.currentTimeMillis();

        try {
            // Build the prompt with system and user messages
            // targetLength is now guaranteed to be one of: "short", "medium", "long"
            String systemPrompt = String.format(
                    "You are an expert technical writer. Summarize the following article in a clear, concise way.\n" +
                            "Target length: %s\n" +
                            "Return only the summary text.",
                    targetLength
            );

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(content)
            ));

            ChatResponse response = chatModel.call(prompt);

            // Safely extract summary with null checks
            String summary;
            if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                String text = response.getResult().getOutput().getText();
                if (text == null || text.isBlank()) {
                    throw new IllegalStateException("Received empty response from AI model");
                }
                summary = text;
            } else {
                throw new IllegalStateException("Invalid response structure from AI model");
            }

            long latency = System.currentTimeMillis() - start;
            String model = "ollama"; // Could extract from response if available

            // Save summary to database
            Summary summaryEntity = new Summary();
            summaryEntity.setOriginalContent(content);
            summaryEntity.setSummary(summary);
            summaryEntity.setSourceUrl(sourceUrl);
            summaryEntity.setArticleTitle(articleTitle);
            summaryEntity.setTargetLength(targetLength);
            summaryEntity.setModel(model);
            summaryEntity.setLatencyMs(latency);

            Summary savedSummary = summaryRepository.save(summaryEntity);

            // Record success metrics
            meterRegistry.counter("summarizer.requests.total",
                    "source", source,
                    "targetLength", targetLength,
                    "status", "success").increment();

            logger.info("Summarization success source={} targetLength={} model={} latencyMs={} urlDomain={} hasUrl={}",
                    source,
                    targetLength,
                    model,
                    latency,
                    extractDomain(sourceUrl),
                    sourceUrl != null && !sourceUrl.isBlank());

            // Return response with summary ID
            SummarizeResponse summarizeResponse = new SummarizeResponse(summary, model, latency);
            summarizeResponse.setId(savedSummary.getId());
            summarizeResponse.setCreatedAt(savedSummary.getCreatedAt());
            summarizeResponse.setSourceUrl(sourceUrl);
            summarizeResponse.setArticleTitle(articleTitle);

            return summarizeResponse;
        } catch (Exception ex) {
            String errorType = classifyError(ex);

            meterRegistry.counter("summarizer.requests.total",
                    "source", source,
                    "targetLength", targetLength,
                    "status", "error").increment();

            meterRegistry.counter("summarizer.errors.total",
                    "source", source,
                    "targetLength", targetLength,
                    "errorType", errorType).increment();

            logger.warn("Summarization failed source={} targetLength={} errorType={} message={}",
                    source,
                    targetLength,
                    errorType,
                    ex.getMessage());

            throw ex;
        } finally {
            sample.stop(Timer.builder("summarizer.latency")
                    .description("End-to-end latency for summarization requests")
                    .tag("source", source)
                    .tag("targetLength", targetLength)
                    .register(meterRegistry));
        }
    }

    private String classifyError(Exception ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String message = cause.getMessage() != null ? cause.getMessage() : "";
        String lowered = message.toLowerCase();

        if (lowered.contains("connection") || lowered.contains("connect") || lowered.contains("refused")) {
            return "ollama";
        }
        if (cause instanceof java.io.IOException) {
            return "url_fetch";
        }
        if (lowered.contains("sql") || lowered.contains("database") || lowered.contains("h2")) {
            return "db";
        }
        if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException) {
            return "validation";
        }
        return "other";
    }

    private String extractDomain(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null ? host : "";
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private SummarizeResponse summarizeDemo(SummarizeRequest request) throws IOException {
        String requestedTargetLength = request.getTargetLength();
        final String targetLength = com.example.aiarticlesummarizer.api.dto.TargetLength
                .fromString(requestedTargetLength)
                .getValue();

        String content;
        String sourceUrl = request.getUrl();
        String articleTitle = null;

        if (sourceUrl != null && !sourceUrl.isBlank()) {
            content = "[Demo mode: URL content not fetched]";
            articleTitle = "Demo Article";  // Skip URL fetch in demo to avoid 403/timeouts
        } else {
            content = Objects.requireNonNull(request.getContent(), "content must not be null");
        }

        String summary = demoModeService.getMockSummary(targetLength);

        Summary summaryEntity = new Summary();
        summaryEntity.setOriginalContent(content);
        summaryEntity.setSummary(summary);
        summaryEntity.setSourceUrl(sourceUrl);
        summaryEntity.setArticleTitle(articleTitle != null ? articleTitle : "Demo Article");
        summaryEntity.setTargetLength(targetLength);
        summaryEntity.setModel("demo");
        summaryEntity.setLatencyMs(150);
        Summary savedSummary = summaryRepository.save(summaryEntity);

        SummarizeResponse response = new SummarizeResponse(summary, "demo", 150);
        response.setId(savedSummary.getId());
        response.setCreatedAt(savedSummary.getCreatedAt());
        response.setSourceUrl(sourceUrl);
        response.setArticleTitle(articleTitle != null ? articleTitle : "Demo Article");
        return response;
    }
}

