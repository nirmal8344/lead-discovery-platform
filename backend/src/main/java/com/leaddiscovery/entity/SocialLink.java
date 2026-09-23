package com.leaddiscovery.entity;

import com.leaddiscovery.entity.enums.ContactVerificationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "social_links")
public class SocialLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 50)
    private String platform;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_page_id")
    private SourcePage sourcePage;

    @Column(name = "source_domain")
    private String sourceDomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private ContactVerificationStatus verificationStatus = ContactVerificationStatus.UNVERIFIED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public SourcePage getSourcePage() { return sourcePage; }
    public void setSourcePage(SourcePage sourcePage) { this.sourcePage = sourcePage; }
    public String getSourceDomain() { return sourceDomain; }
    public void setSourceDomain(String sourceDomain) { this.sourceDomain = sourceDomain; }
    public ContactVerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(ContactVerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
