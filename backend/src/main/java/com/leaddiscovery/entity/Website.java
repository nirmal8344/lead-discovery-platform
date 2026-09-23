package com.leaddiscovery.entity;

import com.leaddiscovery.entity.enums.WebsiteStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "websites")
public class Website {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "normalized_url", nullable = false, columnDefinition = "TEXT")
    private String normalizedUrl;

    @Column(name = "is_official", nullable = false)
    private Boolean isOfficial = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WebsiteStatus status = WebsiteStatus.PENDING;

    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "website", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SourcePage> sourcePages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getNormalizedUrl() { return normalizedUrl; }
    public void setNormalizedUrl(String normalizedUrl) { this.normalizedUrl = normalizedUrl; }
    public Boolean getIsOfficial() { return isOfficial; }
    public void setIsOfficial(Boolean isOfficial) { this.isOfficial = isOfficial; }
    public WebsiteStatus getStatus() { return status; }
    public void setStatus(WebsiteStatus status) { this.status = status; }
    public LocalDateTime getLastCrawledAt() { return lastCrawledAt; }
    public void setLastCrawledAt(LocalDateTime lastCrawledAt) { this.lastCrawledAt = lastCrawledAt; }
    public List<SourcePage> getSourcePages() { return sourcePages; }
    public void setSourcePages(List<SourcePage> sourcePages) { this.sourcePages = sourcePages; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
