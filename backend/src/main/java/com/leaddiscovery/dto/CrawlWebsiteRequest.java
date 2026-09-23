package com.leaddiscovery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public class CrawlWebsiteRequest {

    @NotBlank(message = "URL cannot be blank")
    @URL(message = "Please provide a valid URL (including http:// or https://)")
    private String url;

    @Min(value = 1, message = "maxPages must be at least 1")
    @Max(value = 20, message = "maxPages cannot exceed 20")
    private int maxPages = 5;

    private Integer maxCrawlDepth;
    private String requiredFields;

    public CrawlWebsiteRequest() {
    }

    public CrawlWebsiteRequest(String url, int maxPages) {
        this.url = url;
        this.maxPages = maxPages;
    }

    public CrawlWebsiteRequest(String url, int maxPages, Integer maxCrawlDepth, String requiredFields) {
        this.url = url;
        this.maxPages = maxPages;
        this.maxCrawlDepth = maxCrawlDepth;
        this.requiredFields = requiredFields;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public Integer getMaxCrawlDepth() {
        return maxCrawlDepth;
    }

    public void setMaxCrawlDepth(Integer maxCrawlDepth) {
        this.maxCrawlDepth = maxCrawlDepth;
    }

    public String getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(String requiredFields) {
        this.requiredFields = requiredFields;
    }
}
