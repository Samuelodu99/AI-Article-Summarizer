package com.example.aiarticlesummarizer.api.dto;

public class AdminStatsDto {

    private long totalUsers;
    private long totalSummaries;

    public AdminStatsDto(long totalUsers, long totalSummaries) {
        this.totalUsers = totalUsers;
        this.totalSummaries = totalSummaries;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalSummaries() {
        return totalSummaries;
    }

    public void setTotalSummaries(long totalSummaries) {
        this.totalSummaries = totalSummaries;
    }
}
