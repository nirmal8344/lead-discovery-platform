package com.leaddiscovery.dto;

import com.leaddiscovery.entity.*;
import com.leaddiscovery.entity.enums.ContactVerificationStatus;
import com.leaddiscovery.entity.enums.ProcessingStatus;
import com.leaddiscovery.entity.enums.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LeadDetailDto {

    private Long id;
    private Long taskId;
    private String businessName;
    private String normalizedName;
    private String category;
    private String address;
    private String city;
    private String state;
    private String country;
    private String sourceUrl;
    private String officialDomain;
    private VerificationStatus verificationStatus;
    private BigDecimal confidenceScore;
    private String missingFields;
    private ProcessingStatus processingStatus;
    private LocalDateTime scrapingTimestamp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<WebsiteDto> websites = new ArrayList<>();
    private List<SourcePageDto> sourcePages = new ArrayList<>();
    private List<EmailAddressDto> emailAddresses = new ArrayList<>();
    private List<PhoneNumberDto> phoneNumbers = new ArrayList<>();
    private List<SocialLinkDto> socialLinks = new ArrayList<>();
    private List<ContactDto> contacts = new ArrayList<>();

    public LeadDetailDto() {
    }

    public static class WebsiteDto {
        private Long id;
        private String url;
        private String normalizedUrl;
        private Boolean isOfficial;
        private String status;
        private LocalDateTime lastCrawledAt;

        public WebsiteDto(Long id, String url, String normalizedUrl, Boolean isOfficial, String status, LocalDateTime lastCrawledAt) {
            this.id = id;
            this.url = url;
            this.normalizedUrl = normalizedUrl;
            this.isOfficial = isOfficial;
            this.status = status;
            this.lastCrawledAt = lastCrawledAt;
        }

        public Long getId() { return id; }
        public String getUrl() { return url; }
        public String getNormalizedUrl() { return normalizedUrl; }
        public Boolean getIsOfficial() { return isOfficial; }
        public String getStatus() { return status; }
        public LocalDateTime getLastCrawledAt() { return lastCrawledAt; }
    }

    public static class SourcePageDto {
        private Long id;
        private String pageUrl;
        private String pageType;
        private Integer httpStatus;
        private LocalDateTime fetchedAt;

        public SourcePageDto(Long id, String pageUrl, String pageType, Integer httpStatus, LocalDateTime fetchedAt) {
            this.id = id;
            this.pageUrl = pageUrl;
            this.pageType = pageType;
            this.httpStatus = httpStatus;
            this.fetchedAt = fetchedAt;
        }

        public Long getId() { return id; }
        public String getPageUrl() { return pageUrl; }
        public String getPageType() { return pageType; }
        public Integer getHttpStatus() { return httpStatus; }
        public LocalDateTime getFetchedAt() { return fetchedAt; }
    }

    public static class EmailAddressDto {
        private Long id;
        private String rawValue;
        private String normalizedValue;
        private String sourcePageUrl;
        private String sourceDomain;
        private String verificationStatus;
        private LocalDateTime extractedAt;

        public EmailAddressDto(Long id, String rawValue, String normalizedValue, String sourcePageUrl,
                               String sourceDomain, String verificationStatus, LocalDateTime extractedAt) {
            this.id = id;
            this.rawValue = rawValue;
            this.normalizedValue = normalizedValue;
            this.sourcePageUrl = sourcePageUrl;
            this.sourceDomain = sourceDomain;
            this.verificationStatus = verificationStatus;
            this.extractedAt = extractedAt;
        }

        public Long getId() { return id; }
        public String getRawValue() { return rawValue; }
        public String getNormalizedValue() { return normalizedValue; }
        public String getSourcePageUrl() { return sourcePageUrl; }
        public String getSourceDomain() { return sourceDomain; }
        public String getVerificationStatus() { return verificationStatus; }
        public LocalDateTime getExtractedAt() { return extractedAt; }
    }

    public static class PhoneNumberDto {
        private Long id;
        private String rawValue;
        private String normalizedValue;
        private String phoneType;
        private String sourcePageUrl;
        private String sourceDomain;
        private String verificationStatus;
        private LocalDateTime extractedAt;

        public PhoneNumberDto(Long id, String rawValue, String normalizedValue, String phoneType,
                              String sourcePageUrl, String sourceDomain, String verificationStatus, LocalDateTime extractedAt) {
            this.id = id;
            this.rawValue = rawValue;
            this.normalizedValue = normalizedValue;
            this.phoneType = phoneType;
            this.sourcePageUrl = sourcePageUrl;
            this.sourceDomain = sourceDomain;
            this.verificationStatus = verificationStatus;
            this.extractedAt = extractedAt;
        }

        public Long getId() { return id; }
        public String getRawValue() { return rawValue; }
        public String getNormalizedValue() { return normalizedValue; }
        public String getPhoneType() { return phoneType; }
        public String getSourcePageUrl() { return sourcePageUrl; }
        public String getSourceDomain() { return sourceDomain; }
        public String getVerificationStatus() { return verificationStatus; }
        public LocalDateTime getExtractedAt() { return extractedAt; }
    }

    public static class SocialLinkDto {
        private Long id;
        private String platform;
        private String url;
        private String sourcePageUrl;
        private String sourceDomain;
        private String verificationStatus;
        private LocalDateTime extractedAt;

        public SocialLinkDto(Long id, String platform, String url, String sourcePageUrl,
                             String sourceDomain, String verificationStatus, LocalDateTime extractedAt) {
            this.id = id;
            this.platform = platform;
            this.url = url;
            this.sourcePageUrl = sourcePageUrl;
            this.sourceDomain = sourceDomain;
            this.verificationStatus = verificationStatus;
            this.extractedAt = extractedAt;
        }

        public Long getId() { return id; }
        public String getPlatform() { return platform; }
        public String getUrl() { return url; }
        public String getSourcePageUrl() { return sourcePageUrl; }
        public String getSourceDomain() { return sourceDomain; }
        public String getVerificationStatus() { return verificationStatus; }
        public LocalDateTime getExtractedAt() { return extractedAt; }
    }

    public static class ContactDto {
        private Long id;
        private String contactPerson;
        private String role;
        private String sourcePageUrl;

        public ContactDto(Long id, String contactPerson, String role, String sourcePageUrl) {
            this.id = id;
            this.contactPerson = contactPerson;
            this.role = role;
            this.sourcePageUrl = sourcePageUrl;
        }

        public Long getId() { return id; }
        public String getContactPerson() { return contactPerson; }
        public String getRole() { return role; }
        public String getSourcePageUrl() { return sourcePageUrl; }
    }

    public static LeadDetailDto fromEntity(Organization org) {
        LeadDetailDto dto = new LeadDetailDto();
        dto.setId(org.getId());
        dto.setTaskId(org.getScrapingTask() != null ? org.getScrapingTask().getId() : null);
        dto.setBusinessName(org.getBusinessName());
        dto.setNormalizedName(org.getNormalizedName());
        dto.setCategory(org.getCategory());
        dto.setAddress(org.getAddress());
        dto.setCity(org.getCity());
        dto.setState(org.getState());
        dto.setCountry(org.getCountry());
        dto.setSourceUrl(org.getSourceUrl());
        dto.setVerificationStatus(org.getVerificationStatus());
        dto.setConfidenceScore(org.getConfidenceScore());
        dto.setMissingFields(org.getMissingFields());
        dto.setProcessingStatus(org.getProcessingStatus());
        dto.setScrapingTimestamp(org.getScrapingTimestamp());
        dto.setCreatedAt(org.getCreatedAt());
        dto.setUpdatedAt(org.getUpdatedAt());

        String officialDomain = null;
        if (org.getWebsites() != null) {
            for (Website w : org.getWebsites()) {
                dto.getWebsites().add(new WebsiteDto(
                        w.getId(), w.getUrl(), w.getNormalizedUrl(), w.getIsOfficial(),
                        w.getStatus() != null ? w.getStatus().name() : null, w.getLastCrawledAt()
                ));
                if (Boolean.TRUE.equals(w.getIsOfficial()) && officialDomain == null) {
                    officialDomain = w.getUrl();
                }
                if (w.getSourcePages() != null) {
                    for (SourcePage sp : w.getSourcePages()) {
                        dto.getSourcePages().add(new SourcePageDto(
                                sp.getId(), sp.getPageUrl(),
                                sp.getPageType() != null ? sp.getPageType().name() : "OTHER",
                                sp.getHttpStatus(), sp.getFetchedAt()
                        ));
                    }
                }
            }
        }
        dto.setOfficialDomain(officialDomain);

        if (org.getEmailAddresses() != null) {
            for (EmailAddress e : org.getEmailAddresses()) {
                String sourceUrl = e.getSourcePage() != null ? e.getSourcePage().getPageUrl() : null;
                String sourceDomain = e.getSourceDomain();
                if (sourceDomain == null && e.getSourcePage() != null) {
                    sourceDomain = extractDomainFromUrl(e.getSourcePage().getPageUrl());
                }
                String status = e.getVerificationStatus() != null ? e.getVerificationStatus().name() : "UNVERIFIED";
                dto.getEmailAddresses().add(new EmailAddressDto(
                        e.getId(), e.getRawValue(), e.getNormalizedValue(), sourceUrl,
                        sourceDomain, status, e.getCreatedAt()
                ));
            }
        }

        if (org.getPhoneNumbers() != null) {
            for (PhoneNumber p : org.getPhoneNumbers()) {
                String sourceUrl = p.getSourcePage() != null ? p.getSourcePage().getPageUrl() : null;
                String sourceDomain = p.getSourceDomain();
                if (sourceDomain == null && p.getSourcePage() != null) {
                    sourceDomain = extractDomainFromUrl(p.getSourcePage().getPageUrl());
                }
                String status = p.getVerificationStatus() != null ? p.getVerificationStatus().name() : "UNVERIFIED";
                dto.getPhoneNumbers().add(new PhoneNumberDto(
                        p.getId(), p.getRawValue(), p.getNormalizedValue(),
                        p.getPhoneType() != null ? p.getPhoneType().name() : null,
                        sourceUrl, sourceDomain, status, p.getCreatedAt()
                ));
            }
        }

        if (org.getSocialLinks() != null) {
            for (SocialLink s : org.getSocialLinks()) {
                String sourceUrl = s.getSourcePage() != null ? s.getSourcePage().getPageUrl() : null;
                String sourceDomain = s.getSourceDomain();
                if (sourceDomain == null && s.getSourcePage() != null) {
                    sourceDomain = extractDomainFromUrl(s.getSourcePage().getPageUrl());
                }
                String status = s.getVerificationStatus() != null ? s.getVerificationStatus().name() : "UNVERIFIED";
                dto.getSocialLinks().add(new SocialLinkDto(
                        s.getId(), s.getPlatform(), s.getUrl(), sourceUrl,
                        sourceDomain, status, s.getCreatedAt()
                ));
            }
        }

        if (org.getContacts() != null) {
            for (Contact c : org.getContacts()) {
                String sourceUrl = c.getSourcePage() != null ? c.getSourcePage().getPageUrl() : null;
                dto.getContacts().add(new ContactDto(
                        c.getId(), c.getContactPerson(), c.getRole(), sourceUrl
                ));
            }
        }

        return dto;
    }

    private static String extractDomainFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            if (host == null) return null;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return null;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
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
    public String getOfficialDomain() { return officialDomain; }
    public void setOfficialDomain(String officialDomain) { this.officialDomain = officialDomain; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getMissingFields() { return missingFields; }
    public void setMissingFields(String missingFields) { this.missingFields = missingFields; }
    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(ProcessingStatus processingStatus) { this.processingStatus = processingStatus; }
    public LocalDateTime getScrapingTimestamp() { return scrapingTimestamp; }
    public void setScrapingTimestamp(LocalDateTime scrapingTimestamp) { this.scrapingTimestamp = scrapingTimestamp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<WebsiteDto> getWebsites() { return websites; }
    public void setWebsites(List<WebsiteDto> websites) { this.websites = websites; }
    public List<SourcePageDto> getSourcePages() { return sourcePages; }
    public void setSourcePages(List<SourcePageDto> sourcePages) { this.sourcePages = sourcePages; }
    public List<EmailAddressDto> getEmailAddresses() { return emailAddresses; }
    public void setEmailAddresses(List<EmailAddressDto> emailAddresses) { this.emailAddresses = emailAddresses; }
    public List<PhoneNumberDto> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(List<PhoneNumberDto> phoneNumbers) { this.phoneNumbers = phoneNumbers; }
    public List<SocialLinkDto> getSocialLinks() { return socialLinks; }
    public void setSocialLinks(List<SocialLinkDto> socialLinks) { this.socialLinks = socialLinks; }
    public List<ContactDto> getContacts() { return contacts; }
    public void setContacts(List<ContactDto> contacts) { this.contacts = contacts; }
}
