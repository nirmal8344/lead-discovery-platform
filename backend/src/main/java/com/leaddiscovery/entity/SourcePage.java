package com.leaddiscovery.entity;

import com.leaddiscovery.entity.enums.SourcePageType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "source_pages")
public class SourcePage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "website_id", nullable = false)
    private Website website;

    @Column(name = "page_url", nullable = false, columnDefinition = "TEXT")
    private String pageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "page_type", nullable = false, length = 30)
    private SourcePageType pageType = SourcePageType.OTHER;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Website getWebsite() { return website; }
    public void setWebsite(Website website) { this.website = website; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public SourcePageType getPageType() { return pageType; }
    public void setPageType(SourcePageType pageType) { this.pageType = pageType; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
