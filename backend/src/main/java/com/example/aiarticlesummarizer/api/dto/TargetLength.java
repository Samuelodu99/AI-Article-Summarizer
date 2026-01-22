package com.example.aiarticlesummarizer.api.dto;

/**
 * Enumeration of valid target length values for article summarization.
 * Prevents prompt injection attacks by restricting input to predefined values.
 */
public enum TargetLength {
    SHORT("short"),
    MEDIUM("medium"),
    LONG("long");

    private final String value;

    TargetLength(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Safely converts a string to TargetLength enum, defaulting to MEDIUM if invalid.
     */
    public static TargetLength fromString(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        String normalized = value.toLowerCase().trim();
        for (TargetLength length : values()) {
            if (length.value.equals(normalized)) {
                return length;
            }
        }
        return MEDIUM; // Default fallback
    }
}
