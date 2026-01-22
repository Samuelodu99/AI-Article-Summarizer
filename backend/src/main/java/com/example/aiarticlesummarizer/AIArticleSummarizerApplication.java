package com.example.aiarticlesummarizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class AIArticleSummarizerApplication {

    private static final Logger logger = LoggerFactory.getLogger(AIArticleSummarizerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AIArticleSummarizerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkConfiguration() {
        String ollamaBaseUrl = System.getenv("OLLAMA_BASE_URL");
        if (ollamaBaseUrl == null || ollamaBaseUrl.isBlank()) {
            ollamaBaseUrl = "http://localhost:11434";
        }
        String model = System.getenv("OLLAMA_MODEL");
        if (model == null || model.isBlank()) {
            model = "llama3";
        }
        logger.info("✅ Ollama configured - Base URL: {}, Model: {}", ollamaBaseUrl, model);
        logger.info("ℹ️  Make sure Ollama is running: ollama serve (or start Ollama desktop app)");
        logger.info("ℹ️  Download a model if needed: ollama pull {}", model);
    }
}

