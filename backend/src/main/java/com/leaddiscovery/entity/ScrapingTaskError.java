package com.leaddiscovery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scraping_task_errors")
public class ScrapingTaskError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private ScrapingTask scrapingTask;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(length = 255)
    private String domain;

    @Column(name = "error_type", nullable = false, length = 100)
    private String errorType;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(length = 100)
    private String stage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public ScrapingTaskError() {}

    public ScrapingTaskError(ScrapingTask scrapingTask, String url, String domain, String errorType, String errorMessage, String stage) {
        this.scrapingTask = scrapingTask;
        this.url = url;
        this.domain = domain;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.stage = stage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ScrapingTask getScrapingTask() { return scrapingTask; }
    public void setScrapingTask(ScrapingTask scrapingTask) { this.scrapingTask = scrapingTask; }
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
