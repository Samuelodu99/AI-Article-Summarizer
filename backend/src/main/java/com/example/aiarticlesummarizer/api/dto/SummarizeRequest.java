package com.example.aiarticlesummarizer.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ValidSummarizeRequest
public class SummarizeRequest {

    @Size(max = 10000)
    private String content; // Optional if URL is provided

    @Size(max = 2048)
    private String url; // Optional if content is provided

    @Pattern(regexp = "^(short|medium|long)$", message = "targetLength must be one of: short, medium, long")
    private String targetLength; // Restricted to enum values to prevent prompt injection

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTargetLength() {
        return targetLength;
    }

    public void setTargetLength(String targetLength) {
        this.targetLength = targetLength;
    }
}

