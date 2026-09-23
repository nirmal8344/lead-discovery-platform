package com.leaddiscovery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CreateTaskRequest {

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Keyword/category is required")
    private String keyword;

    @Min(value = 1, message = "maxResults must be at least 1")
    @Max(value = 1000, message = "maxResults cannot exceed 1000")
    private Integer maxResults = 50;

    @Min(value = 1, message = "maxPagesPerSite must be at least 1")
    @Max(value = 20, message = "maxPagesPerSite cannot exceed 20")
    private Integer maxPagesPerSite = 5;

    /** Search radius in kilometers (e.g. 5, 10, 25, 50, 100) */
    @Min(value = 1, message = "searchRadiusKm must be at least 1")
    @Max(value = 500, message = "searchRadiusKm cannot exceed 500")
    private Integer searchRadiusKm;

    /** Max crawl depth from root page */
    @Min(value = 1, message = "maxCrawlDepth must be at least 1")
    @Max(value = 10, message = "maxCrawlDepth cannot exceed 10")
    private Integer maxCrawlDepth = 3;

    /** Comma-separated list, e.g. "email,phone,website,whatsapp" */
    private String requiredFields;

    /** Comma-separated list of optional filter descriptors */
    private String optionalFilters;

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getMaxResults() { return maxResults; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
    public Integer getMaxPagesPerSite() { return maxPagesPerSite; }
    public void setMaxPagesPerSite(Integer maxPagesPerSite) { this.maxPagesPerSite = maxPagesPerSite; }
    public Integer getSearchRadiusKm() { return searchRadiusKm; }
    public void setSearchRadiusKm(Integer searchRadiusKm) { this.searchRadiusKm = searchRadiusKm; }
    public Integer getMaxCrawlDepth() { return maxCrawlDepth; }
    public void setMaxCrawlDepth(Integer maxCrawlDepth) { this.maxCrawlDepth = maxCrawlDepth; }
    public String getRequiredFields() { return requiredFields; }
    public void setRequiredFields(String requiredFields) { this.requiredFields = requiredFields; }
    public String getOptionalFilters() { return optionalFilters; }
    public void setOptionalFilters(String optionalFilters) { this.optionalFilters = optionalFilters; }
}
