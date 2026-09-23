package com.leaddiscovery.entity;

import com.leaddiscovery.entity.enums.TaskStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scraping_tasks")
public class ScrapingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String keyword;

    @Column(name = "max_results", nullable = false)
    private Integer maxResults = 50;

    @Column(name = "max_pages_per_site", nullable = false)
    private Integer maxPagesPerSite = 5;

    @Column(name = "search_radius_km")
    private Integer searchRadiusKm;

    @Column(name = "max_crawl_depth", nullable = false)
    private Integer maxCrawlDepth = 3;

    @Column(name = "required_fields", columnDefinition = "TEXT")
    private String requiredFields;

    @Column(name = "optional_filters", columnDefinition = "TEXT")
    private String optionalFilters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status = TaskStatus.CREATED;

    @Column(name = "progress_percentage", nullable = false)
    private Integer progressPercentage = 0;

    @Column(name = "current_stage", length = 150)
    private String currentStage;

    @Column(name = "discovered_businesses", nullable = false)
    private Integer discoveredBusinesses = 0;

    @Column(name = "processed_websites", nullable = false)
    private Integer processedWebsites = 0;

    @Column(name = "leads_saved", nullable = false)
    private Integer leadsSaved = 0;

    @Column(name = "failed_records", nullable = false)
    private Integer failedRecords = 0;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    public Integer getDiscoveredBusinesses() { return discoveredBusinesses; }
    public void setDiscoveredBusinesses(Integer discoveredBusinesses) { this.discoveredBusinesses = discoveredBusinesses; }
    public Integer getProcessedWebsites() { return processedWebsites; }
    public void setProcessedWebsites(Integer processedWebsites) { this.processedWebsites = processedWebsites; }
    public Integer getLeadsSaved() { return leadsSaved; }
    public void setLeadsSaved(Integer leadsSaved) { this.leadsSaved = leadsSaved; }
    public Integer getFailedRecords() { return failedRecords; }
    public void setFailedRecords(Integer failedRecords) { this.failedRecords = failedRecords; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
