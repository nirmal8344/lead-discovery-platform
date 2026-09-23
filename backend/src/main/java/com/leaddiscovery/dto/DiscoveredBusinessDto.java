package com.leaddiscovery.dto;

public class DiscoveredBusinessDto {

    private String businessName;
    private String websiteUrl;
    private String sourceUrl;
    private String discoverySource;
    private Double confidence;
    private String status;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;

    public DiscoveredBusinessDto() {
    }

    public DiscoveredBusinessDto(String businessName, String websiteUrl, String sourceUrl,
                                 String discoverySource, Double confidence, String status) {
        this(businessName, websiteUrl, sourceUrl, discoverySource, confidence, status, null, null, null);
    }

    public DiscoveredBusinessDto(String businessName, String websiteUrl, String sourceUrl,
                                 String discoverySource, Double confidence, String status,
                                 Double latitude, Double longitude, Double distanceKm) {
        this.businessName = businessName;
        this.websiteUrl = websiteUrl;
        this.sourceUrl = sourceUrl;
        this.discoverySource = discoverySource;
        this.confidence = confidence;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getDiscoverySource() {
        return discoverySource;
    }

    public void setDiscoverySource(String discoverySource) {
        this.discoverySource = discoverySource;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
}
