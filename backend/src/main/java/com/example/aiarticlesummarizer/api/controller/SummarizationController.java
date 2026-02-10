package com.example.aiarticlesummarizer.api.controller;

import com.example.aiarticlesummarizer.api.dto.SummarizeRequest;
import com.example.aiarticlesummarizer.api.dto.SummarizeResponse;
import com.example.aiarticlesummarizer.service.SummarizationService;
import com.example.aiarticlesummarizer.service.StreamingSummarizationService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/summarize")
// CORS is configured globally via CorsConfig - no need for @CrossOrigin here
public class SummarizationController {

    private final SummarizationService summarizationService;
    private final StreamingSummarizationService streamingSummarizationService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SummarizationController(SummarizationService summarizationService, StreamingSummarizationService streamingSummarizationService) {
        this.summarizationService = summarizationService;
        this.streamingSummarizationService = streamingSummarizationService;
    }

    @PostMapping
    public ResponseEntity<SummarizeResponse> summarize(@Valid @RequestBody SummarizeRequest request) {
        try {
            SummarizeResponse response = summarizationService.summarize(request);
            return ResponseEntity.ok(response);
        } catch (java.io.IOException e) {
            // Re-throw as RuntimeException so Spring's exception handler can catch it
            // The GlobalExceptionHandler will convert it to a proper error response
            throw new RuntimeException(e);
        }
    }

    /** Servlet async timeout is configured in application.yml (e.g. 15 min). Emitter timeout must be >= that. */
    private static final long STREAM_EMITTER_TIMEOUT_MS = 900_000L; // 15 minutes

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter summarizeStream(@Valid @RequestBody SummarizeRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_EMITTER_TIMEOUT_MS);

        emitter.onTimeout(() -> {
            sendStreamError(emitter, "Request timed out. The summary took too long to generate. Try a shorter article or summary length.");
        });
        emitter.onError((ex) -> {
            String message = streamErrorMessage(ex);
            sendStreamError(emitter, message);
        });

        executor.execute(() -> {
            try {
                Flux<String> stream = streamingSummarizationService.summarizeStream(request);
                
                stream.subscribe(
                    chunk -> {
                        if (chunk == null) return;
                        try {
                            emitter.send(SseEmitter.event()
                                    .data(chunk)
                                    .name("chunk"));
                        } catch (IOException e) {
                            sendStreamError(emitter, e.getMessage());
                        }
                    },
                    error -> {
                        String message = streamErrorMessage(error);
                        sendStreamError(emitter, message);
                    },
                    () -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .data("[DONE]")
                                    .name("done"));
                            emitter.complete();
                        } catch (IOException e) {
                            sendStreamError(emitter, e.getMessage());
                        }
                    }
                );
            } catch (Exception e) {
                // Any exception (e.g. 403 during URL fetch) before/during stream start:
                // send one SSE error event so client gets a clear message and response stays text/event-stream
                String message = streamErrorMessage(e);
                sendStreamError(emitter, message);
            }
        });

        return emitter;
    }

    /**
     * Send a single SSE "error" event with the given message and complete the emitter.
     * Keeps response as text/event-stream so the client can parse it consistently.
     */
    private void sendStreamError(SseEmitter emitter, String message) {
        try {
            String data = (message != null && !message.isBlank()) ? message : "An error occurred while generating the summary.";
            emitter.send(SseEmitter.event()
                    .data(data)
                    .name("error"));
            emitter.complete();
        } catch (Exception sendEx) {
            emitter.completeWithError(sendEx);
        }
    }

    /**
     * Build a user-friendly error message from an exception (aligned with GlobalExceptionHandler).
     */
    private String streamErrorMessage(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String msg = cause.getMessage() != null ? cause.getMessage() : "";

        if (cause instanceof java.io.IOException) {
            if (msg.contains("403") || msg.contains("Forbidden")) {
                return "Failed to fetch content from URL. Access forbidden. The website may block automated requests. You can paste the article text in the Text tab instead.";
            }
            if (msg.contains("timeout") || msg.contains("Timeout")) {
                return "Failed to fetch content from URL. Request timed out. The website may be slow or unreachable.";
            }
            if (msg.contains("404") || msg.contains("Not Found")) {
                return "Failed to fetch content from URL. URL not found. Please check the URL and try again.";
            }
            return "Failed to fetch content from URL. " + (msg.length() > 200 ? msg.substring(0, 200) + "…" : msg);
        }
        if (msg.contains("connection") || msg.contains("Connection refused") || msg.contains("connect")) {
            return "Cannot connect to Ollama. Make sure Ollama is running (ollama serve) and accessible at http://localhost:11434";
        }
        if (msg.contains("model") || msg.contains("not found")) {
            return "Model not found. Make sure you've downloaded the model: ollama pull llama3";
        }
        if (msg.contains("timeout") || msg.contains("read timeout")) {
            return "Request timed out. The model may be processing a large input. Please try again.";
        }
        if (msg.contains("database") || msg.contains("SQL") || msg.contains("H2")) {
            return "Database error. Check backend logs for details.";
        }
        return msg.length() > 300 ? msg.substring(0, 300) + "…" : (msg.isBlank() ? "An error occurred." : msg);
    }
}

