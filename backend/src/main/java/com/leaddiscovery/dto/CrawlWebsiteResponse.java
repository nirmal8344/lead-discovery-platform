package com.leaddiscovery.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CrawlWebsiteResponse {

    private String rootUrl;
    private String domain;
    private int pagesCrawled;
    private List<PageExtractDto> pages;
    private Set<String> aggregatedEmails;
    private Set<String> aggregatedPhoneNumbers;
    private Set<String> aggregatedWhatsAppNumbers;
    private Map<String, String> aggregatedSocialLinks;
    private Set<String> aggregatedAddresses;
    private OrganizationIdentity organizationIdentity;
    private LocalDateTime crawledAt;

    public CrawlWebsiteResponse() {
    }

    public CrawlWebsiteResponse(String rootUrl, String domain, int pagesCrawled,
                                List<PageExtractDto> pages, Set<String> aggregatedEmails,
                                Set<String> aggregatedPhoneNumbers,
                                Map<String, String> aggregatedSocialLinks,
                                Set<String> aggregatedAddresses,
                                LocalDateTime crawledAt) {
        this(rootUrl, domain, pagesCrawled, pages, aggregatedEmails, aggregatedPhoneNumbers,
                null, aggregatedSocialLinks, aggregatedAddresses, null, crawledAt);
    }

    public CrawlWebsiteResponse(String rootUrl, String domain, int pagesCrawled,
                                List<PageExtractDto> pages, Set<String> aggregatedEmails,
                                Set<String> aggregatedPhoneNumbers,
                                Map<String, String> aggregatedSocialLinks,
                                Set<String> aggregatedAddresses,
                                OrganizationIdentity organizationIdentity,
                                LocalDateTime crawledAt) {
        this(rootUrl, domain, pagesCrawled, pages, aggregatedEmails, aggregatedPhoneNumbers,
                null, aggregatedSocialLinks, aggregatedAddresses, organizationIdentity, crawledAt);
    }

    public CrawlWebsiteResponse(String rootUrl, String domain, int pagesCrawled,
                                List<PageExtractDto> pages, Set<String> aggregatedEmails,
                                Set<String> aggregatedPhoneNumbers,
                                Set<String> aggregatedWhatsAppNumbers,
                                Map<String, String> aggregatedSocialLinks,
                                Set<String> aggregatedAddresses,
                                OrganizationIdentity organizationIdentity,
                                LocalDateTime crawledAt) {
        this.rootUrl = rootUrl;
        this.domain = domain;
        this.pagesCrawled = pagesCrawled;
        this.pages = pages;
        this.aggregatedEmails = aggregatedEmails;
        this.aggregatedPhoneNumbers = aggregatedPhoneNumbers;
        this.aggregatedWhatsAppNumbers = aggregatedWhatsAppNumbers;
        this.aggregatedSocialLinks = aggregatedSocialLinks;
        this.aggregatedAddresses = aggregatedAddresses;
        this.organizationIdentity = organizationIdentity;
        this.crawledAt = crawledAt;
    }

    public String getRootUrl() { return rootUrl; }
    public void setRootUrl(String rootUrl) { this.rootUrl = rootUrl; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public int getPagesCrawled() { return pagesCrawled; }
    public void setPagesCrawled(int pagesCrawled) { this.pagesCrawled = pagesCrawled; }
    public List<PageExtractDto> getPages() { return pages; }
    public void setPages(List<PageExtractDto> pages) { this.pages = pages; }
    public Set<String> getAggregatedEmails() { return aggregatedEmails; }
    public void setAggregatedEmails(Set<String> aggregatedEmails) { this.aggregatedEmails = aggregatedEmails; }
    public Set<String> getAggregatedPhoneNumbers() { return aggregatedPhoneNumbers; }
    public void setAggregatedPhoneNumbers(Set<String> aggregatedPhoneNumbers) { this.aggregatedPhoneNumbers = aggregatedPhoneNumbers; }
    public Set<String> getAggregatedWhatsAppNumbers() { return aggregatedWhatsAppNumbers; }
    public void setAggregatedWhatsAppNumbers(Set<String> aggregatedWhatsAppNumbers) { this.aggregatedWhatsAppNumbers = aggregatedWhatsAppNumbers; }
    public Map<String, String> getAggregatedSocialLinks() { return aggregatedSocialLinks; }
    public void setAggregatedSocialLinks(Map<String, String> aggregatedSocialLinks) { this.aggregatedSocialLinks = aggregatedSocialLinks; }
    public Set<String> getAggregatedAddresses() { return aggregatedAddresses; }
    public void setAggregatedAddresses(Set<String> aggregatedAddresses) { this.aggregatedAddresses = aggregatedAddresses; }
    public OrganizationIdentity getOrganizationIdentity() { return organizationIdentity; }
    public void setOrganizationIdentity(OrganizationIdentity organizationIdentity) { this.organizationIdentity = organizationIdentity; }
    public LocalDateTime getCrawledAt() { return crawledAt; }
    public void setCrawledAt(LocalDateTime crawledAt) { this.crawledAt = crawledAt; }
}
