package com.example.aiarticlesummarizer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${spring.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:3000}")
    private String allowedOrigins;

    @Value("${spring.cors.allowed-methods:GET,POST,OPTIONS,DELETE}")
    private String allowedMethods;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Parse comma-separated origins from configuration
        // Trim whitespace from each origin and filter out empty strings
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
        
        // Parse allowed methods
        List<String> methods = Arrays.stream(allowedMethods.split(","))
                .map(String::trim)
                .filter(method -> !method.isEmpty())
                .collect(Collectors.toList());
        
        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods(methods.toArray(new String[0]))
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
