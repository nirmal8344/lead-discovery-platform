package com.leaddiscovery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.leaddiscovery.entity.ScrapingTask;
import com.leaddiscovery.entity.enums.TaskStatus;

import java.time.LocalDateTime;

public class TaskProgressDto {

    private Long taskId;
    private TaskStatus status;
    private Integer totalDiscovered;
    private Integer totalCrawled;
    private Integer totalSaved;
    private Integer totalFailed;
    private Integer progressPercentage;
    private String currentStage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    public TaskProgressDto() {
    }

    public TaskProgressDto(Long taskId, TaskStatus status, Integer totalDiscovered,
                           Integer totalCrawled, Integer totalSaved, Integer totalFailed,
                           Integer progressPercentage, String currentStage,
                           LocalDateTime startedAt, LocalDateTime completedAt, String errorMessage) {
        this.taskId = taskId;
        this.status = status;
        this.totalDiscovered = totalDiscovered;
        this.totalCrawled = totalCrawled;
        this.totalSaved = totalSaved;
        this.totalFailed = totalFailed;
        this.progressPercentage = progressPercentage;
        this.currentStage = currentStage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
    }

    public static TaskProgressDto fromEntity(ScrapingTask task) {
        return new TaskProgressDto(
                task.getId(),
                task.getStatus(),
                task.getDiscoveredBusinesses(),
                task.getProcessedWebsites(),
                task.getLeadsSaved(),
                task.getFailedRecords(),
                task.getProgressPercentage(),
                task.getCurrentStage(),
                task.getStartTime(),
                task.getEndTime(),
                task.getErrorMessage()
        );
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Integer getTotalDiscovered() { return totalDiscovered; }
    public void setTotalDiscovered(Integer totalDiscovered) { this.totalDiscovered = totalDiscovered; }
    public Integer getTotalCrawled() { return totalCrawled; }
    public void setTotalCrawled(Integer totalCrawled) { this.totalCrawled = totalCrawled; }
    public Integer getTotalSaved() { return totalSaved; }
    public void setTotalSaved(Integer totalSaved) { this.totalSaved = totalSaved; }
    public Integer getTotalFailed() { return totalFailed; }
    public void setTotalFailed(Integer totalFailed) { this.totalFailed = totalFailed; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    @JsonProperty("startTime")
    public LocalDateTime getStartTime() { return startedAt; }

    @JsonProperty("endTime")
    public LocalDateTime getEndTime() { return completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
