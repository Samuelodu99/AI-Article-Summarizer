package com.example.aiarticlesummarizer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Provides mock summarization responses when demo mode is enabled.
 * Used for portfolio deployments where Ollama is not available.
 */
@Service
public class DemoModeService {

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    public boolean isDemoMode() {
        return demoMode;
    }

    /**
     * Returns a mock summary based on target length.
     */
    public String getMockSummary(String targetLength) {
        return switch (targetLength != null ? targetLength.toLowerCase() : "medium") {
            case "short" -> "This is a demo summary. The AI Article Summarizer uses Spring AI with Ollama to generate concise summaries. In demo mode, this pre-generated response is shown. Deploy with DEMO_MODE=false and connect to Ollama for real AI summaries.";
            case "long" -> """
                This is a demo summary demonstrating the AI Article Summarizer's capabilities.
                
                The application is built with Java, Spring Boot, and Spring AI on the backend, with React and Vite on the frontend. It supports both text input and URL fetching for automatic article extraction.
                
                In demo mode, this pre-generated response is displayed so recruiters and visitors can experience the full UI and workflow without running Ollama locally. For production use, set DEMO_MODE=false and connect to an Ollama instance for real AI-powered summarization.
                
                Features include streaming responses, summary history, export to PDF/Markdown, user authentication, and an admin dashboard.""";
            default -> """
                This is a demo summary. The AI Article Summarizer uses Spring AI with Ollama to generate summaries from text or URLs.
                
                In demo mode, this pre-generated response is shown so you can explore the app without local setup. For real AI summaries, deploy with DEMO_MODE=false and connect to Ollama.""";
        };
    }

    /**
     * Returns a Flux that emits the mock summary in chunks with realistic streaming delay.
     */
    public Flux<String> streamMockSummary(String targetLength) {
        String fullSummary = getMockSummary(targetLength);
        List<String> chunks = splitIntoChunks(fullSummary);
        return Flux.fromIterable(chunks)
                .delayElements(Duration.ofMillis(25))
                .doOnComplete(() -> {});
    }

    private List<String> splitIntoChunks(String text) {
        // Split by word boundaries, emitting 1-3 words per chunk for natural streaming
        String[] words = text.split("\\s+");
        List<String> chunks = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int wordsInChunk = 0;
        for (String word : words) {
            if (current.length() > 0) current.append(" ");
            current.append(word);
            wordsInChunk++;
            if (wordsInChunk >= 2 || current.length() > 40) {
                chunks.add(current.toString());
                current = new StringBuilder();
                wordsInChunk = 0;
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }
}
