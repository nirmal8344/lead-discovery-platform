package com.leaddiscovery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scraping_logs")
public class ScrapingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scraping_task_id", nullable = false)
    private ScrapingTask scrapingTask;

    @Column(nullable = false, length = 20)
    private String level = "INFO";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ScrapingTask getScrapingTask() { return scrapingTask; }
    public void setScrapingTask(ScrapingTask scrapingTask) { this.scrapingTask = scrapingTask; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
