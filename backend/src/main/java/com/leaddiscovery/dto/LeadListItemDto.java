package com.leaddiscovery.dto;

import com.leaddiscovery.entity.EmailAddress;
import com.leaddiscovery.entity.Organization;
import com.leaddiscovery.entity.PhoneNumber;
import com.leaddiscovery.entity.Website;
import com.leaddiscovery.entity.enums.ContactVerificationStatus;
import com.leaddiscovery.entity.enums.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LeadListItemDto {

    private Long leadId;
    private String businessName;
    private String city;
    private String category;
    private String websiteUrl;
    private String primaryEmail;
    private String primaryPhone;
    private VerificationStatus verificationStatus;
    private BigDecimal confidenceScore;
    private LocalDateTime createdAt;

    public LeadListItemDto() {
    }

    public LeadListItemDto(Long leadId, String businessName, String city, String category,
                           String websiteUrl, String primaryEmail, String primaryPhone,
                           VerificationStatus verificationStatus, BigDecimal confidenceScore,
                           LocalDateTime createdAt) {
        this.leadId = leadId;
        this.businessName = businessName;
        this.city = city;
        this.category = category;
        this.websiteUrl = websiteUrl;
        this.primaryEmail = primaryEmail;
        this.primaryPhone = primaryPhone;
        this.verificationStatus = verificationStatus;
        this.confidenceScore = confidenceScore;
        this.createdAt = createdAt;
    }

    public static LeadListItemDto fromEntity(Organization org) {
        String websiteUrl = null;
        if (org.getWebsites() != null && !org.getWebsites().isEmpty()) {
            for (Website w : org.getWebsites()) {
                if (Boolean.TRUE.equals(w.getIsOfficial())) {
                    websiteUrl = w.getUrl();
                    break;
                }
            }
            if (websiteUrl == null) {
                websiteUrl = org.getWebsites().get(0).getUrl();
            }
        }

        // Prioritize verified corporate email
        String primaryEmail = null;
        if (org.getEmailAddresses() != null && !org.getEmailAddresses().isEmpty()) {
            for (EmailAddress email : org.getEmailAddresses()) {
                if (email.getVerificationStatus() == ContactVerificationStatus.VERIFIED) {
                    primaryEmail = email.getNormalizedValue();
                    break;
                }
            }
            if (primaryEmail == null) {
                // Fallback to first email if none strictly marked VERIFIED
                primaryEmail = org.getEmailAddresses().get(0).getNormalizedValue();
            }
        }

        // Prioritize verified phone
        String primaryPhone = null;
        if (org.getPhoneNumbers() != null && !org.getPhoneNumbers().isEmpty()) {
            for (PhoneNumber phone : org.getPhoneNumbers()) {
                if (phone.getVerificationStatus() == ContactVerificationStatus.VERIFIED) {
                    primaryPhone = phone.getNormalizedValue();
                    break;
                }
            }
            if (primaryPhone == null) {
                primaryPhone = org.getPhoneNumbers().get(0).getNormalizedValue();
            }
        }

        return new LeadListItemDto(
                org.getId(),
                org.getBusinessName(),
                org.getCity(),
                org.getCategory(),
                websiteUrl,
                primaryEmail,
                primaryPhone,
                org.getVerificationStatus(),
                org.getConfidenceScore(),
                org.getCreatedAt()
        );
    }

    public Long getId() { return leadId; }
    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getPrimaryEmail() { return primaryEmail; }
    public void setPrimaryEmail(String primaryEmail) { this.primaryEmail = primaryEmail; }
    public String getPrimaryPhone() { return primaryPhone; }
    public void setPrimaryPhone(String primaryPhone) { this.primaryPhone = primaryPhone; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
