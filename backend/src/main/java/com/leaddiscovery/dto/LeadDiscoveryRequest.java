package com.leaddiscovery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class LeadDiscoveryRequest {

    @NotBlank(message = "Location cannot be blank")
    private String location;

    @NotBlank(message = "Keyword/category cannot be blank")
    private String keyword;

    @Min(value = 1, message = "maxResults must be at least 1")
    @Max(value = 100, message = "maxResults cannot exceed 100")
    private int maxResults = 10;

    @Min(value = 1, message = "searchRadiusKm must be at least 1")
    @Max(value = 500, message = "searchRadiusKm cannot exceed 500")
    private Integer searchRadiusKm;

    public LeadDiscoveryRequest() {
    }

    public LeadDiscoveryRequest(String location, String keyword, int maxResults) {
        this(location, keyword, maxResults, null);
    }

    public LeadDiscoveryRequest(String location, String keyword, int maxResults, Integer searchRadiusKm) {
        this.location = location;
        this.keyword = keyword;
        this.maxResults = maxResults;
        this.searchRadiusKm = searchRadiusKm;
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

    public Integer getSearchRadiusKm() {
        return searchRadiusKm;
    }

    public void setSearchRadiusKm(Integer searchRadiusKm) {
        this.searchRadiusKm = searchRadiusKm;
    }
}
