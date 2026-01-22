package com.example.aiarticlesummarizer.api.dto;

public class SummarizeResponse {

    private String summary;
    private String model;
    private long latencyMs;

    public SummarizeResponse() {
    }

    public SummarizeResponse(String summary, String model, long latencyMs) {
        this.summary = summary;
        this.model = model;
        this.latencyMs = latencyMs;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }
}

