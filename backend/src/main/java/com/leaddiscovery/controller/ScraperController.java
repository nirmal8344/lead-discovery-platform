package com.leaddiscovery.controller;

import com.leaddiscovery.dto.ScrapeUrlRequest;
import com.leaddiscovery.dto.ScrapeUrlResponse;
import com.leaddiscovery.service.WebScraperService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scrape")
public class ScraperController {

    private final WebScraperService webScraperService;

    public ScraperController(WebScraperService webScraperService) {
        this.webScraperService = webScraperService;
    }

    @PostMapping("/url")
    public ResponseEntity<ScrapeUrlResponse> scrapeUrl(@Valid @RequestBody ScrapeUrlRequest request) {
        ScrapeUrlResponse response = webScraperService.scrapeUrl(request.getUrl());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/crawl")
    public ResponseEntity<com.leaddiscovery.dto.CrawlWebsiteResponse> crawlWebsite(
            @Valid @RequestBody com.leaddiscovery.dto.CrawlWebsiteRequest request) {
        int maxDepth = request.getMaxCrawlDepth() != null ? request.getMaxCrawlDepth() : 3;
        com.leaddiscovery.dto.CrawlWebsiteResponse response =
                webScraperService.crawlWebsite(request.getUrl(), request.getMaxPages(), maxDepth, request.getRequiredFields());
        return ResponseEntity.ok(response);
    }
}
