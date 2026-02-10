package com.example.aiarticlesummarizer.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${spring.ai.ollama.chat.options.model:llama3}")
    private String model;

    @Bean
    public ChatModel chatModel() {
        // Create OllamaApi
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();
        
        // Create OllamaChatOptions with the model
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(model)
                .build();
        
        // Create required dependencies
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        ModelManagementOptions modelManagementOptions = ModelManagementOptions.builder().build();
        
        // Create and return OllamaChatModel
        return new OllamaChatModel(ollamaApi, options, toolCallingManager, observationRegistry, modelManagementOptions);
    }
}
