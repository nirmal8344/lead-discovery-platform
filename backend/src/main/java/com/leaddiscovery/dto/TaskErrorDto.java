package com.leaddiscovery.dto;

import com.leaddiscovery.entity.ScrapingTaskError;
import java.time.LocalDateTime;

public class TaskErrorDto {
    private Long id;
    private Long taskId;
    private String url;
    private String domain;
    private String errorType;
    private String errorMessage;
    private String stage;
    private LocalDateTime createdAt;

    public TaskErrorDto() {}

    public static TaskErrorDto fromEntity(ScrapingTaskError error) {
        TaskErrorDto dto = new TaskErrorDto();
        dto.setId(error.getId());
        dto.setTaskId(error.getScrapingTask() != null ? error.getScrapingTask().getId() : null);
        dto.setUrl(error.getUrl());
        dto.setDomain(error.getDomain());
        dto.setErrorType(error.getErrorType());
        dto.setErrorMessage(error.getErrorMessage());
        dto.setStage(error.getStage());
        dto.setCreatedAt(error.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
