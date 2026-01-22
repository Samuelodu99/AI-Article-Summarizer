package com.example.aiarticlesummarizer.service;

import com.example.aiarticlesummarizer.api.dto.SummarizeRequest;
import com.example.aiarticlesummarizer.api.dto.SummarizeResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class SummarizationService {

    private final ChatModel chatModel;

    public SummarizationService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public SummarizeResponse summarize(SummarizeRequest request) {
        long start = System.currentTimeMillis();

        // Safely validate and normalize targetLength to prevent prompt injection
        String requestedTargetLength = request.getTargetLength();
        final String targetLength = com.example.aiarticlesummarizer.api.dto.TargetLength
                .fromString(requestedTargetLength)
                .getValue();
        final String content = Objects.requireNonNull(request.getContent(), "content must not be null");

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

        return new SummarizeResponse(summary, model, latency);
    }
}

