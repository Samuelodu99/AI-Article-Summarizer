package com.example.aiarticlesummarizer.service;

import com.example.aiarticlesummarizer.api.dto.SummarizeRequest;
import com.example.aiarticlesummarizer.model.Summary;
import com.example.aiarticlesummarizer.repository.SummaryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class StreamingSummarizationService {

    private static final Logger logger = LoggerFactory.getLogger(StreamingSummarizationService.class);

    private final ChatModel chatModel;
    private final UrlFetchingService urlFetchingService;
    private final SummaryRepository summaryRepository;
    private final MeterRegistry meterRegistry;

    public StreamingSummarizationService(ChatModel chatModel,
                                         UrlFetchingService urlFetchingService,
                                         SummaryRepository summaryRepository,
                                         MeterRegistry meterRegistry) {
        this.chatModel = chatModel;
        this.urlFetchingService = urlFetchingService;
        this.summaryRepository = summaryRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public Flux<String> summarizeStream(SummarizeRequest request) throws IOException {
        // Safely validate and normalize targetLength to prevent prompt injection
        String requestedTargetLength = request.getTargetLength();
        final String targetLength = com.example.aiarticlesummarizer.api.dto.TargetLength
                .fromString(requestedTargetLength)
                .getValue();

        // Handle URL fetching if URL is provided
        final String sourceUrl = request.getUrl();
        final String articleTitle;
        final String content;

        if (sourceUrl != null && !sourceUrl.isBlank()) {
            // Fetch content from URL
            String fetchedContent = urlFetchingService.fetchArticleContent(sourceUrl);
            articleTitle = urlFetchingService.fetchArticleTitle(sourceUrl);
            // Truncate content if too long (to prevent token limits)
            content = fetchedContent.length() > 8000 
                    ? fetchedContent.substring(0, 8000) + "... [truncated]"
                    : fetchedContent;
        } else {
            // Use provided content
            String providedContent = Objects.requireNonNull(request.getContent(), "content must not be null");
            articleTitle = null;
            // Truncate content if too long (to prevent token limits)
            content = providedContent.length() > 8000 
                    ? providedContent.substring(0, 8000) + "... [truncated]"
                    : providedContent;
        }

        final String source = (sourceUrl != null && !sourceUrl.isBlank()) ? "url" : "text";
        Timer.Sample sample = Timer.start(meterRegistry);

        // Build the prompt
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

        // Stream the response
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        AtomicReference<StringBuilder> fullSummary = new AtomicReference<>(new StringBuilder());
        final String finalContent = content;
        final String finalArticleTitle = articleTitle;
        final String finalSourceUrl = sourceUrl;

        return chatModel.stream(prompt)
                .doOnSubscribe(subscription -> {
                    startTime.set(System.currentTimeMillis());
                    meterRegistry.counter("summarizer.streaming.requests.total",
                            "source", source,
                            "targetLength", targetLength).increment();
                })
                .map(response -> {
                    if (response.getResult() != null && response.getResult().getOutput() != null) {
                        String chunk = response.getResult().getOutput().getText();
                        if (chunk != null) {
                            fullSummary.get().append(chunk);
                            return chunk;
                        }
                    }
                    return "";
                })
                .filter(chunk -> !chunk.isEmpty())
                .doOnComplete(() -> {
                    // Save summary to database after streaming completes
                    try {
                        long latency = System.currentTimeMillis() - startTime.get();
                        String model = "ollama";
                        String completeSummary = fullSummary.get().toString();

                        Summary summaryEntity = new Summary();
                        summaryEntity.setOriginalContent(finalContent);
                        summaryEntity.setSummary(completeSummary);
                        summaryEntity.setSourceUrl(finalSourceUrl);
                        summaryEntity.setArticleTitle(finalArticleTitle);
                        summaryEntity.setTargetLength(targetLength);
                        summaryEntity.setModel(model);
                        summaryEntity.setLatencyMs(latency);
                        summaryEntity.setCreatedAt(LocalDateTime.now());

                        summaryRepository.save(summaryEntity);
                        logger.info("Streaming summarization success source={} targetLength={} latencyMs={} urlDomain={} hasUrl={}",
                                source,
                                targetLength,
                                latency,
                                extractDomain(finalSourceUrl),
                                finalSourceUrl != null && !finalSourceUrl.isBlank());
                    } catch (Exception e) {
                        // Log error but don't fail the stream
                        logger.warn("Error saving streamed summary: {}", e.getMessage(), e);
                    }
                })
                .doOnError(ex -> {
                    String errorType = classifyError(ex);
                    meterRegistry.counter("summarizer.streaming.errors.total",
                            "source", source,
                            "targetLength", targetLength,
                            "errorType", errorType).increment();

                    logger.warn("Streaming summarization failed source={} targetLength={} errorType={} message={}",
                            source,
                            targetLength,
                            errorType,
                            ex.getMessage());
                })
                .doFinally(signalType -> {
                    String status = (signalType == SignalType.ON_COMPLETE) ? "success" : "error";
                    sample.stop(Timer.builder("summarizer.streaming.latency")
                            .description("End-to-end latency for streaming summarization requests")
                            .tag("source", source)
                            .tag("targetLength", targetLength)
                            .tag("status", status)
                            .register(meterRegistry));
                });
    }

    private String classifyError(Throwable ex) {
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
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            return host != null ? host : "";
        } catch (Exception e) {
            return "";
        }
    }
}
