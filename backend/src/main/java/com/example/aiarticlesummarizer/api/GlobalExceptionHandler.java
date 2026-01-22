package com.example.aiarticlesummarizer.api;

import com.example.aiarticlesummarizer.api.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message;
        if (ex.getBindingResult().getAllErrors().isEmpty()) {
            message = "Invalid request.";
        } else {
            // getDefaultMessage() can return null, so provide a fallback
            String defaultMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            message = (defaultMessage != null && !defaultMessage.isBlank())
                    ? defaultMessage
                    : "Invalid request. Please check your input and try again.";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Log the full exception for debugging
        logger.error("Error during summarization", ex);
        
        // Provide a more helpful error message based on common issues
        String message = "Summarization failed. ";
        String exceptionMessage = ex.getMessage();
        
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("connection") || exceptionMessage.contains("Connection refused") || exceptionMessage.contains("connect")) {
                message += "Cannot connect to Ollama. Make sure Ollama is running (ollama serve) and accessible at http://localhost:11434";
            } else if (exceptionMessage.contains("model") || exceptionMessage.contains("not found") || exceptionMessage.contains("model not found")) {
                message += "Model not found. Make sure you've downloaded the model: ollama pull llama3";
            } else if (exceptionMessage.contains("timeout") || exceptionMessage.contains("read timeout")) {
                message += "Request timed out. The model may be processing a large input. Please try again.";
            } else {
                message += "Error: " + exceptionMessage;
            }
        } else {
            message += "Check backend logs for details.";
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(message));
    }
}

