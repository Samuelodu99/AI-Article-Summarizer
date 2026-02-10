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

    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(java.io.IOException ex) {
        logger.error("IO error during URL fetching", ex);
        String message = "Failed to fetch content from URL. ";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("timeout") || ex.getMessage().contains("Timeout")) {
                message += "Request timed out. The website may be slow or unreachable.";
            } else if (ex.getMessage().contains("404") || ex.getMessage().contains("Not Found")) {
                message += "URL not found. Please check the URL and try again.";
            } else if (ex.getMessage().contains("403") || ex.getMessage().contains("Forbidden")) {
                message += "Access forbidden. The website may block automated requests. You can paste the article text in the Text tab instead.";
            } else {
                message += ex.getMessage();
            }
        } else {
            message += "Please check the URL and try again.";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Invalid request.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        // Check if this RuntimeException wraps an IOException
        Throwable cause = ex.getCause();
        if (cause instanceof java.io.IOException) {
            return handleIOException((java.io.IOException) cause);
        }
        
        // Otherwise, treat as generic exception
        return handleGeneric(ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Log the full exception for debugging
        logger.error("Error during summarization", ex);
        
        // Provide a more helpful error message based on common issues
        String message = "Summarization failed. ";
        String exceptionMessage = ex.getMessage();
        
        // Check the cause as well
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            exceptionMessage = cause.getMessage();
        }
        
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("connection") || exceptionMessage.contains("Connection refused") || exceptionMessage.contains("connect")) {
                message += "Cannot connect to Ollama. Make sure Ollama is running (ollama serve) and accessible at http://localhost:11434";
            } else if (exceptionMessage.contains("model") || exceptionMessage.contains("not found") || exceptionMessage.contains("model not found")) {
                message += "Model not found. Make sure you've downloaded the model: ollama pull llama3";
            } else if (exceptionMessage.contains("timeout") || exceptionMessage.contains("read timeout")) {
                message += "Request timed out. The model may be processing a large input. Please try again.";
            } else if (exceptionMessage.contains("database") || exceptionMessage.contains("SQL") || exceptionMessage.contains("H2")) {
                message += "Database error. Check backend logs for details.";
            } else {
                message += "Error: " + exceptionMessage;
            }
        } else {
            message += "Check backend logs for details.";
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(message));
    }
}

