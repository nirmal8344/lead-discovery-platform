package com.leaddiscovery.service;

import com.leaddiscovery.discovery.BusinessDiscoveryProvider;
import com.leaddiscovery.discovery.UrlFilterUtils;
import com.leaddiscovery.dto.CrawlWebsiteResponse;
import com.leaddiscovery.dto.DiscoveredBusinessDto;
import com.leaddiscovery.dto.LeadDiscoveryRequest;
import com.leaddiscovery.dto.LeadDiscoveryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LeadDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(LeadDiscoveryService.class);

    private final List<BusinessDiscoveryProvider> providers;
    private final WebScraperService webScraperService;

    public LeadDiscoveryService(List<BusinessDiscoveryProvider> providers, WebScraperService webScraperService) {
        this.providers = providers != null ? providers : Collections.emptyList();
        this.webScraperService = webScraperService;
    }

    /**
     * Discovers businesses using location and keyword across enabled discovery providers.
     */
    public LeadDiscoveryResponse discoverLeads(LeadDiscoveryRequest request) {
        String location = request.getLocation().trim();
        String keyword = request.getKeyword().trim();
        int maxResults = Math.max(1, Math.min(request.getMaxResults(), 100));

        Integer radiusKm = request.getSearchRadiusKm();
        log.info("[DISCOVERY_START] Starting lead discovery: keyword='{}', location='{}', maxResults={}, radiusKm={}",
                keyword, location, maxResults, radiusKm);

        List<DiscoveredBusinessDto> discoveredBusinesses = new ArrayList<>();
        Set<String> seenDomains = new HashSet<>();
        List<String> warnings = new ArrayList<>();

        if (providers.isEmpty()) {
            warnings.add("No active business discovery providers configured.");
        }

        for (BusinessDiscoveryProvider provider : providers) {
            if (!provider.isEnabled()) {
                continue;
            }

            try {
                log.info("[DISCOVERY_PROVIDER] Querying provider '{}' for '{} in {}' (radius={} km)",
                        provider.getProviderName(), keyword, location, radiusKm != null ? radiusKm : "N/A");
                List<DiscoveredBusinessDto> results = provider.discover(location, keyword, maxResults, radiusKm);
                int providerRawCount = results != null ? results.size() : 0;
                int providerAcceptedCount = 0;

                if (results != null) {
                    for (DiscoveredBusinessDto business : results) {
                        if (discoveredBusinesses.size() >= maxResults) {
                            break;
                        }

                        // If distance is reliably computed and exceeds radius, filter it out
                        if (radiusKm != null && radiusKm > 0 && business.getDistanceKm() != null && business.getDistanceKm() > radiusKm) {
                            log.info("[RADIUS_EXCEEDED] Skipping candidate '{}' at distance {:.2f} km (> {} km radius)",
                                    business.getBusinessName(), business.getDistanceKm(), radiusKm);
                            continue;
                        }

                        String normalizedUrl = UrlFilterUtils.normalizeUrl(business.getWebsiteUrl());
                        if (normalizedUrl == null || !UrlFilterUtils.isValidOfficialWebsite(normalizedUrl)) {
                            log.debug("[DISCOVERY_REJECTED] Rejected candidate URL '{}' from provider '{}'",
                                    business.getWebsiteUrl(), provider.getProviderName());
                            continue;
                        }

                        String domain = UrlFilterUtils.extractDomain(normalizedUrl);
                        if (domain.isBlank() || seenDomains.contains(domain)) {
                            continue;
                        }

                        seenDomains.add(domain);
                        business.setWebsiteUrl(normalizedUrl);
                        discoveredBusinesses.add(business);
                        providerAcceptedCount++;

                        log.info("[DISCOVERY_ACCEPTED] Provider '{}' accepted candidate '{}' ({})",
                                provider.getProviderName(), business.getBusinessName(), normalizedUrl);
                    }
                }

                log.info("[DISCOVERY_PROVIDER_RESULT] Provider '{}': rawCount={}, acceptedCount={}",
                        provider.getProviderName(), providerRawCount, providerAcceptedCount);

            } catch (Exception e) {
                log.error("[DISCOVERY_PROVIDER_ERROR] Error running discovery provider '{}': {}", provider.getProviderName(), e.getMessage());
                warnings.add("Provider '" + provider.getProviderName() + "' encountered an error: " + e.getMessage());
            }

            if (discoveredBusinesses.size() >= maxResults) {
                break;
            }
        }

        if (discoveredBusinesses.isEmpty()) {
            warnings.add("No candidate businesses found matching keyword '" + keyword + "' and location '" + location + "'.");
        }

        log.info("[DISCOVERY_COMPLETE] Completed lead discovery: {} total unique businesses discovered", discoveredBusinesses.size());

        return new LeadDiscoveryResponse(
                location,
                keyword,
                discoveredBusinesses.size(),
                discoveredBusinesses,
                warnings
        );
    }

    /**
     * Bridges discovered business to the existing multi-page website crawler.
     */
    public CrawlWebsiteResponse crawlDiscoveredWebsite(String websiteUrl, int maxPages) {
        return webScraperService.crawlWebsite(websiteUrl, maxPages);
    }

    public CrawlWebsiteResponse crawlDiscoveredWebsite(String websiteUrl, int maxPages, int maxCrawlDepth) {
        return webScraperService.crawlWebsite(websiteUrl, maxPages, maxCrawlDepth);
    }

    public CrawlWebsiteResponse crawlDiscoveredWebsite(String websiteUrl, int maxPages, int maxCrawlDepth, String requiredFields) {
        return webScraperService.crawlWebsite(websiteUrl, maxPages, maxCrawlDepth, requiredFields);
    }
}
