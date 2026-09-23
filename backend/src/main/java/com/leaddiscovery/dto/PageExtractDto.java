package com.leaddiscovery.dto;

import com.leaddiscovery.entity.enums.SourcePageType;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class PageExtractDto {

    private String url;
    private SourcePageType pageType;
    private String title;
    private int httpStatus;
    private Set<String> emails;
    private Set<String> phoneNumbers;
    private Map<String, String> socialLinks;
    private Set<String> addresses;
    private LocalDateTime fetchedAt;

    public PageExtractDto() {
    }

    public PageExtractDto(String url, SourcePageType pageType, String title, int httpStatus,
                          Set<String> emails, Set<String> phoneNumbers,
                          Map<String, String> socialLinks, Set<String> addresses,
                          LocalDateTime fetchedAt) {
        this.url = url;
        this.pageType = pageType;
        this.title = title;
        this.httpStatus = httpStatus;
        this.emails = emails;
        this.phoneNumbers = phoneNumbers;
        this.socialLinks = socialLinks;
        this.addresses = addresses;
        this.fetchedAt = fetchedAt;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public SourcePageType getPageType() { return pageType; }
    public void setPageType(SourcePageType pageType) { this.pageType = pageType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getHttpStatus() { return httpStatus; }
    public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }
    public Set<String> getEmails() { return emails; }
    public void setEmails(Set<String> emails) { this.emails = emails; }
    public Set<String> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(Set<String> phoneNumbers) { this.phoneNumbers = phoneNumbers; }
    public Map<String, String> getSocialLinks() { return socialLinks; }
    public void setSocialLinks(Map<String, String> socialLinks) { this.socialLinks = socialLinks; }
    public Set<String> getAddresses() { return addresses; }
    public void setAddresses(Set<String> addresses) { this.addresses = addresses; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
}
