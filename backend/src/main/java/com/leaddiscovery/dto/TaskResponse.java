package com.leaddiscovery.dto;

import com.leaddiscovery.entity.ScrapingTask;
import com.leaddiscovery.entity.enums.TaskStatus;
import java.time.LocalDateTime;

public class TaskResponse {

    private Long id;
    private String location;
    private String keyword;
    private Integer maxResults;
    private Integer maxPagesPerSite;
    private Integer searchRadiusKm;
    private Integer maxCrawlDepth;
    private String requiredFields;
    private String optionalFilters;
    private TaskStatus status;
    private Integer progressPercentage;
    private String currentStage;
    private Integer discoveredBusinesses;
    private Integer processedWebsites;
    private Integer leadsSaved;
    private Integer failedRecords;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskResponse fromEntity(ScrapingTask task) {
        TaskResponse r = new TaskResponse();
        r.id = task.getId();
        r.location = task.getLocation();
        r.keyword = task.getKeyword();
        r.maxResults = task.getMaxResults();
        r.maxPagesPerSite = task.getMaxPagesPerSite();
        r.searchRadiusKm = task.getSearchRadiusKm();
        r.maxCrawlDepth = task.getMaxCrawlDepth();
        r.requiredFields = task.getRequiredFields();
        r.optionalFilters = task.getOptionalFilters();
        r.status = task.getStatus();
        r.progressPercentage = task.getProgressPercentage();
        r.currentStage = task.getCurrentStage();
        r.discoveredBusinesses = task.getDiscoveredBusinesses();
        r.processedWebsites = task.getProcessedWebsites();
        r.leadsSaved = task.getLeadsSaved();
        r.failedRecords = task.getFailedRecords();
        r.startTime = task.getStartTime();
        r.endTime = task.getEndTime();
        r.errorMessage = task.getErrorMessage();
        r.createdAt = task.getCreatedAt();
        r.updatedAt = task.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getLocation() { return location; }
    public String getKeyword() { return keyword; }
    public Integer getMaxResults() { return maxResults; }
    public Integer getMaxPagesPerSite() { return maxPagesPerSite; }
    public Integer getSearchRadiusKm() { return searchRadiusKm; }
    public Integer getMaxCrawlDepth() { return maxCrawlDepth; }
    public String getRequiredFields() { return requiredFields; }
    public String getOptionalFilters() { return optionalFilters; }
    public TaskStatus getStatus() { return status; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public String getCurrentStage() { return currentStage; }
    public Integer getDiscoveredBusinesses() { return discoveredBusinesses; }
    public Integer getProcessedWebsites() { return processedWebsites; }
    public Integer getLeadsSaved() { return leadsSaved; }
    public Integer getFailedRecords() { return failedRecords; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
