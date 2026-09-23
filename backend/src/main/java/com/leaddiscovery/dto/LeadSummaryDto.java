package com.leaddiscovery.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public class LeadSummaryDto {

    private Long organizationId;
    private String businessName;
    private String websiteUrl;
    private String city;
    private String category;
    private Set<String> emails;
    private Set<String> phoneNumbers;
    private Map<String, String> socialLinks;
    private String address;
    private BigDecimal confidenceScore;
    private String verificationStatus;
    private int pagesCrawled;

    public LeadSummaryDto() {
    }

    public LeadSummaryDto(Long organizationId, String businessName, String websiteUrl, String city,
                          String category, Set<String> emails, Set<String> phoneNumbers,
                          Map<String, String> socialLinks, String address, BigDecimal confidenceScore,
                          String verificationStatus, int pagesCrawled) {
        this.organizationId = organizationId;
        this.businessName = businessName;
        this.websiteUrl = websiteUrl;
        this.city = city;
        this.category = category;
        this.emails = emails;
        this.phoneNumbers = phoneNumbers;
        this.socialLinks = socialLinks;
        this.address = address;
        this.confidenceScore = confidenceScore;
        this.verificationStatus = verificationStatus;
        this.pagesCrawled = pagesCrawled;
    }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Set<String> getEmails() { return emails; }
    public void setEmails(Set<String> emails) { this.emails = emails; }
    public Set<String> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(Set<String> phoneNumbers) { this.phoneNumbers = phoneNumbers; }
    public Map<String, String> getSocialLinks() { return socialLinks; }
    public void setSocialLinks(Map<String, String> socialLinks) { this.socialLinks = socialLinks; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public int getPagesCrawled() { return pagesCrawled; }
    public void setPagesCrawled(int pagesCrawled) { this.pagesCrawled = pagesCrawled; }
}
