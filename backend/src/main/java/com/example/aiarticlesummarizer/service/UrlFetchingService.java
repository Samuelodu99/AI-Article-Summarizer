package com.example.aiarticlesummarizer.service;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.stream.Collectors;

@Service
public class UrlFetchingService {

    private static final Logger logger = LoggerFactory.getLogger(UrlFetchingService.class);
    private static final int TIMEOUT_MS = 10000; // 10 seconds
    // Browser-like User-Agent and headers to reduce 403 from sites that block simple bots
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE = "en-US,en;q=0.9";
    /** Referrer used when site returns 403 for origin referrer (e.g. science.org). */
    private static final String GOOGLE_REFERRER = "https://www.google.com/";

    /**
     * Fetches and extracts article content from a URL.
     * Attempts to extract main content using common article selectors.
     * On 403, retries once with a search-engine referrer to work around sites that block direct access.
     */
    public String fetchArticleContent(String url) throws IOException {
        logger.info("Fetching content from URL: {}", url);

        // Validate URL
        try {
            new java.net.URI(url).toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL format: " + url);
        }

        String referrer = toOriginReferrer(url);
        Document doc;

        try {
            doc = fetchDocument(url, referrer);
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 403 && !GOOGLE_REFERRER.equals(referrer)) {
                logger.info("Got 403 for URL, retrying with search-engine referrer: {}", url);
                doc = fetchDocument(url, GOOGLE_REFERRER);
            } else {
                throw e;
            }
        }

        // Try multiple strategies to extract article content
        String content = extractArticleContent(doc);
        if (content == null) content = "";

        if (content.trim().isEmpty()) {
            // Fallback: extract all paragraph text
            content = doc.select("p").stream()
                    .map(Element::text)
                    .filter(text -> text.length() > 20) // Filter out very short paragraphs
                    .collect(Collectors.joining("\n\n"));
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IOException("Could not extract article content from URL: " + url);
        }

        logger.info("Extracted {} characters from URL", content.length());
        return content.trim()
                .replaceAll("\\s+", " ") // Normalize whitespace
                .replaceAll("\n{3,}", "\n\n"); // Remove excessive newlines
    }

    /**
     * Performs the HTTP GET with browser-like headers. Used by fetchArticleContent and fetchArticleTitle.
     */
    private Document fetchDocument(String url, String referrer) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept", ACCEPT)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", referrer != null && referrer.contains("google") ? "cross-site" : "none")
                .referrer(referrer != null ? referrer : "")
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get();
    }

    /**
     * Attempts to extract article content using common content selectors.
     */
    private String extractArticleContent(Document doc) {
        // Common article content selectors (in order of preference)
        String[] selectors = {
                "article",
                "[role='article']",
                ".article-content",
                ".post-content",
                ".entry-content",
                ".content",
                "main article",
                "main .content",
                ".article-body",
                ".post-body"
        };

        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                String content = elements.stream()
                        .map(Element::text)
                        .filter(text -> text.length() > 100) // Ensure substantial content
                        .collect(Collectors.joining("\n\n"));
                
                if (content.length() > 200) { // Minimum content length
                    return content;
                }
            }
        }

        return null;
    }

    /**
     * Extracts the article title from the URL.
     */
    public String fetchArticleTitle(String url) throws IOException {
        try {
            String referrer = toOriginReferrer(url);
            Document doc;
            try {
                doc = fetchDocument(url, referrer);
            } catch (HttpStatusException e) {
                if (e.getStatusCode() == 403 && !GOOGLE_REFERRER.equals(referrer)) {
                    doc = fetchDocument(url, GOOGLE_REFERRER);
                } else {
                    throw e;
                }
            }

            // Try multiple title selectors
            String title = doc.select("meta[property='og:title']").attr("content");
            if (title.isEmpty()) {
                title = doc.select("meta[name='twitter:title']").attr("content");
            }
            if (title.isEmpty()) {
                title = doc.title();
            }

            return title.trim();
        } catch (Exception e) {
            logger.warn("Could not fetch title from URL: {}", url, e);
            return "Untitled Article";
        }
    }

    /** Build a referrer string from the URL origin (e.g. https://www.science.org) to look more like a browser. */
    private static String toOriginReferrer(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme != null && host != null) {
                int port = uri.getPort();
                if (port <= 0 || port == 80 && "http".equals(scheme) || port == 443 && "https".equals(scheme)) {
                    return scheme + "://" + host + "/";
                }
                return scheme + "://" + host + ":" + port + "/";
            }
        } catch (Exception ignored) { }
        return "";
    }
}
