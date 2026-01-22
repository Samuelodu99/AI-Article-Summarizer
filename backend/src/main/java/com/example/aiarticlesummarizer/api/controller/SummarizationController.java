package com.example.aiarticlesummarizer.api.controller;

import com.example.aiarticlesummarizer.api.dto.SummarizeRequest;
import com.example.aiarticlesummarizer.api.dto.SummarizeResponse;
import com.example.aiarticlesummarizer.service.SummarizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/summarize")
// CORS is configured globally via CorsConfig - no need for @CrossOrigin here
public class SummarizationController {

    private final SummarizationService summarizationService;

    public SummarizationController(SummarizationService summarizationService) {
        this.summarizationService = summarizationService;
    }

    @PostMapping
    public ResponseEntity<SummarizeResponse> summarize(@Valid @RequestBody SummarizeRequest request) {
        SummarizeResponse response = summarizationService.summarize(request);
        return ResponseEntity.ok(response);
    }
}

