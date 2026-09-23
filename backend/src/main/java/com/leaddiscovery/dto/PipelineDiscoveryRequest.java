package com.leaddiscovery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class PipelineDiscoveryRequest {

    @NotBlank(message = "Location cannot be blank")
    private String location;

    @NotBlank(message = "Keyword/category cannot be blank")
    private String keyword;

    @Min(value = 1, message = "maxResults must be at least 1")
    @Max(value = 100, message = "maxResults cannot exceed 100")
    private int maxResults = 5;

    @Min(value = 1, message = "maxPagesPerSite must be at least 1")
    @Max(value = 20, message = "maxPagesPerSite cannot exceed 20")
    private int maxPagesPerSite = 5;

    private Integer searchRadiusKm;
    private String requiredFields;

    public PipelineDiscoveryRequest() {
    }

    public PipelineDiscoveryRequest(String location, String keyword, int maxResults, int maxPagesPerSite) {
        this(location, keyword, maxResults, maxPagesPerSite, null, null);
    }

    public PipelineDiscoveryRequest(String location, String keyword, int maxResults, int maxPagesPerSite,
                                    Integer searchRadiusKm, String requiredFields) {
        this.location = location;
        this.keyword = keyword;
        this.maxResults = maxResults;
        this.maxPagesPerSite = maxPagesPerSite;
        this.searchRadiusKm = searchRadiusKm;
        this.requiredFields = requiredFields;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getMaxPagesPerSite() {
        return maxPagesPerSite;
    }

    public void setMaxPagesPerSite(int maxPagesPerSite) {
        this.maxPagesPerSite = maxPagesPerSite;
    }

    public Integer getSearchRadiusKm() {
        return searchRadiusKm;
    }

    public void setSearchRadiusKm(Integer searchRadiusKm) {
        this.searchRadiusKm = searchRadiusKm;
    }

    public String getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(String requiredFields) {
        this.requiredFields = requiredFields;
    }
}
