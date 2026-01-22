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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${spring.ai.ollama.chat.options.model:llama3}")
    private String model;

    @Bean
    public ChatModel chatModel() {
        // Create RestClient and WebClient builders
        RestClient.Builder restClientBuilder = RestClient.builder();
        WebClient.Builder webClientBuilder = WebClient.builder();
        ResponseErrorHandler errorHandler = new org.springframework.web.client.DefaultResponseErrorHandler();
        
        // Create OllamaApi using builder if available, otherwise use reflection or factory
        OllamaApi ollamaApi;
        try {
            // Try to use builder pattern
            ollamaApi = OllamaApi.builder().baseUrl(baseUrl).build();
        } catch (Exception e) {
            // Fallback: create using package-private constructor via reflection
            try {
                java.lang.reflect.Constructor<OllamaApi> constructor = OllamaApi.class.getDeclaredConstructor(
                    String.class, RestClient.Builder.class, WebClient.Builder.class, ResponseErrorHandler.class
                );
                constructor.setAccessible(true);
                ollamaApi = constructor.newInstance(baseUrl, restClientBuilder, webClientBuilder, errorHandler);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create OllamaApi", ex);
            }
        }
        
        // Create OllamaChatOptions with the model
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(model)
                .build();
        
        // Create ToolCallingManager (required parameter) - use builder or factory
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
        
        // Create ObservationRegistry (required parameter) - use no-op if not available
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        
        // Create ModelManagementOptions (required parameter)
        ModelManagementOptions modelManagementOptions = ModelManagementOptions.builder().build();
        
        // Create OllamaChatModel with required parameters
        return new OllamaChatModel(ollamaApi, options, toolCallingManager, observationRegistry, modelManagementOptions);
    }
}
