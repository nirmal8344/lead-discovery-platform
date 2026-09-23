package com.leaddiscovery.dto;

import java.time.LocalDateTime;
import java.util.Set;

public class ScrapeUrlResponse {

    private String url;
    private String title;
    private Set<String> emails;
    private Set<String> phoneNumbers;
    private java.util.Map<String, String> socialLinks;
    private Set<String> addresses;
    private int httpStatus;
    private LocalDateTime fetchedAt;

    public ScrapeUrlResponse() {
    }

    public ScrapeUrlResponse(String url, String title, Set<String> emails, Set<String> phoneNumbers, int httpStatus, LocalDateTime fetchedAt) {
        this(url, title, emails, phoneNumbers, java.util.Collections.emptyMap(), java.util.Collections.emptySet(), httpStatus, fetchedAt);
    }

    public ScrapeUrlResponse(String url, String title, Set<String> emails, Set<String> phoneNumbers,
                             java.util.Map<String, String> socialLinks, Set<String> addresses,
                             int httpStatus, LocalDateTime fetchedAt) {
        this.url = url;
        this.title = title;
        this.emails = emails;
        this.phoneNumbers = phoneNumbers;
        this.socialLinks = socialLinks;
        this.addresses = addresses;
        this.httpStatus = httpStatus;
        this.fetchedAt = fetchedAt;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<String> getEmails() {
        return emails;
    }

    public void setEmails(Set<String> emails) {
        this.emails = emails;
    }

    public Set<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(Set<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public java.util.Map<String, String> getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(java.util.Map<String, String> socialLinks) {
        this.socialLinks = socialLinks;
    }

    public Set<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<String> addresses) {
        this.addresses = addresses;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}
