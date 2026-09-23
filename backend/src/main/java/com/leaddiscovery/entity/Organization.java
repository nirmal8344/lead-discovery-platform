package com.leaddiscovery.entity;

import com.leaddiscovery.entity.enums.ProcessingStatus;
import com.leaddiscovery.entity.enums.VerificationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scraping_task_id", nullable = false)
    private ScrapingTask scrapingTask;

    @Column(name = "business_name", nullable = false, length = 500)
    private String businessName;

    @Column(name = "normalized_name", nullable = false, length = 500)
    private String normalizedName;

    @Column(length = 255)
    private String category;

    @Column(length = 500)
    private String address;

    @Column(length = 255)
    private String city;

    @Column(length = 255)
    private String state;

    @Column(length = 255)
    private String country;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Column(name = "confidence_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal confidenceScore = BigDecimal.ZERO;

    @Column(name = "missing_fields", columnDefinition = "TEXT")
    private String missingFields;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "scraping_timestamp")
    private LocalDateTime scrapingTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Website> websites = new ArrayList<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhoneNumber> phoneNumbers = new ArrayList<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailAddress> emailAddresses = new ArrayList<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialLink> socialLinks = new ArrayList<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contact> contacts = new ArrayList<>();

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
    public ScrapingTask getScrapingTask() { return scrapingTask; }
    public void setScrapingTask(ScrapingTask scrapingTask) { this.scrapingTask = scrapingTask; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getMissingFields() { return missingFields; }
    public void setMissingFields(String missingFields) { this.missingFields = missingFields; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(ProcessingStatus processingStatus) { this.processingStatus = processingStatus; }
    public LocalDateTime getScrapingTimestamp() { return scrapingTimestamp; }
    public void setScrapingTimestamp(LocalDateTime scrapingTimestamp) { this.scrapingTimestamp = scrapingTimestamp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<Website> getWebsites() { return websites; }
    public void setWebsites(List<Website> websites) { this.websites = websites; }
    public List<PhoneNumber> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) { this.phoneNumbers = phoneNumbers; }
    public List<EmailAddress> getEmailAddresses() { return emailAddresses; }
    public void setEmailAddresses(List<EmailAddress> emailAddresses) { this.emailAddresses = emailAddresses; }
    public List<SocialLink> getSocialLinks() { return socialLinks; }
    public void setSocialLinks(List<SocialLink> socialLinks) { this.socialLinks = socialLinks; }
    public List<Contact> getContacts() { return contacts; }
    public void setContacts(List<Contact> contacts) { this.contacts = contacts; }
}
