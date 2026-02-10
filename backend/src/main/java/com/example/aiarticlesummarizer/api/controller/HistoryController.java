package com.example.aiarticlesummarizer.api.controller;

import com.example.aiarticlesummarizer.api.dto.SummaryHistoryItem;
import com.example.aiarticlesummarizer.model.Summary;
import com.example.aiarticlesummarizer.repository.SummaryRepository;
import com.example.aiarticlesummarizer.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/history")
public class HistoryController {

    private final SummaryRepository summaryRepository;
    private final ExportService exportService;

    public HistoryController(SummaryRepository summaryRepository, ExportService exportService) {
        this.summaryRepository = summaryRepository;
        this.exportService = exportService;
    }

    @GetMapping
    public ResponseEntity<List<SummaryHistoryItem>> getHistory(
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "search", required = false) String search) {
        
        List<Summary> summaries;
        
        if (search != null && !search.isBlank()) {
            summaries = summaryRepository.searchSummaries(search);
        } else {
            summaries = summaryRepository.findTop10ByOrderByCreatedAtDesc();
        }
        
        List<SummaryHistoryItem> items = summaries.stream()
                .limit(limit)
                .map(this::toHistoryItem)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SummaryHistoryItem> getSummaryById(@PathVariable Long id) {
        return summaryRepository.findById(id)
                .map(this::toHistoryItem)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSummary(@PathVariable Long id) {
        if (summaryRepository.existsById(id)) {
            summaryRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllHistory() {
        summaryRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(@PathVariable Long id) {
        var summaryOpt = summaryRepository.findById(id);
        if (summaryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        var summary = summaryOpt.get();
        try {
            byte[] pdfBytes = exportService.exportToPdf(summary);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = (summary.getArticleTitle() != null && !summary.getArticleTitle().isBlank())
                    ? summary.getArticleTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf"
                    : "summary_" + id + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{id}/export/markdown")
    public ResponseEntity<String> exportToMarkdown(@PathVariable Long id) {
        return summaryRepository.findById(id)
                .map(summary -> {
                    String markdown = exportService.exportToMarkdown(summary);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    String filename = (summary.getArticleTitle() != null && !summary.getArticleTitle().isBlank())
                            ? summary.getArticleTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".md"
                            : "summary_" + id + ".md";
                    headers.setContentDispositionFormData("attachment", filename);
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(markdown);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private SummaryHistoryItem toHistoryItem(Summary summary) {
        SummaryHistoryItem item = new SummaryHistoryItem();
        item.setId(summary.getId());
        item.setSummary(summary.getSummary());
        item.setSourceUrl(summary.getSourceUrl());
        item.setArticleTitle(summary.getArticleTitle());
        item.setTargetLength(summary.getTargetLength());
        item.setModel(summary.getModel());
        item.setLatencyMs(summary.getLatencyMs());
        item.setCreatedAt(summary.getCreatedAt());
        
        // Create preview from original content
        String preview = summary.getOriginalContent();
        if (preview != null && preview.length() > 200) {
            preview = preview.substring(0, 200) + "...";
        }
        item.setPreview(preview);
        
        return item;
    }
}
