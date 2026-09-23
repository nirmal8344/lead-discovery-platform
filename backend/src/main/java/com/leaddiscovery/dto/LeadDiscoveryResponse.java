package com.leaddiscovery.dto;

import java.util.List;

public class LeadDiscoveryResponse {

    private String location;
    private String keyword;
    private int totalDiscovered;
    private List<DiscoveredBusinessDto> businesses;
    private List<String> warnings;

    public LeadDiscoveryResponse() {
    }

    public LeadDiscoveryResponse(String location, String keyword, int totalDiscovered,
                                 List<DiscoveredBusinessDto> businesses, List<String> warnings) {
        this.location = location;
        this.keyword = keyword;
        this.totalDiscovered = totalDiscovered;
        this.businesses = businesses;
        this.warnings = warnings;
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

    public int getTotalDiscovered() {
        return totalDiscovered;
    }

    public void setTotalDiscovered(int totalDiscovered) {
        this.totalDiscovered = totalDiscovered;
    }

    public List<DiscoveredBusinessDto> getBusinesses() {
        return businesses;
    }

    public void setBusinesses(List<DiscoveredBusinessDto> businesses) {
        this.businesses = businesses;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
