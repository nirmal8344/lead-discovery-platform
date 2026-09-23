package com.leaddiscovery.dto;

import java.util.List;

public class PipelineDiscoveryResponse {

    private Long taskId;
    private String location;
    private String keyword;
    private int totalDiscovered;
    private int totalCrawled;
    private int totalSaved;
    private int totalFailed;
    private List<LeadSummaryDto> leads;
    private List<String> warnings;
    private String status;

    public PipelineDiscoveryResponse() {
    }

    public PipelineDiscoveryResponse(Long taskId, String location, String keyword,
                                     int totalDiscovered, int totalCrawled, int totalSaved,
                                     int totalFailed, List<LeadSummaryDto> leads,
                                     List<String> warnings, String status) {
        this.taskId = taskId;
        this.location = location;
        this.keyword = keyword;
        this.totalDiscovered = totalDiscovered;
        this.totalCrawled = totalCrawled;
        this.totalSaved = totalSaved;
        this.totalFailed = totalFailed;
        this.leads = leads;
        this.warnings = warnings;
        this.status = status;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public int getTotalDiscovered() { return totalDiscovered; }
    public void setTotalDiscovered(int totalDiscovered) { this.totalDiscovered = totalDiscovered; }
    public int getTotalCrawled() { return totalCrawled; }
    public void setTotalCrawled(int totalCrawled) { this.totalCrawled = totalCrawled; }
    public int getTotalSaved() { return totalSaved; }
    public void setTotalSaved(int totalSaved) { this.totalSaved = totalSaved; }
    public int getTotalFailed() { return totalFailed; }
    public void setTotalFailed(int totalFailed) { this.totalFailed = totalFailed; }
    public List<LeadSummaryDto> getLeads() { return leads; }
    public void setLeads(List<LeadSummaryDto> leads) { this.leads = leads; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
