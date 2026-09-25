package com.leaddiscovery.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaddiscovery.dto.DiscoveredBusinessDto;
import com.leaddiscovery.dto.LeadDiscoveryRequest;
import com.leaddiscovery.dto.LeadDiscoveryResponse;
import com.leaddiscovery.service.LeadDiscoveryService;
import com.leaddiscovery.service.WebScraperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DiscoveryVerificationTest {

    private OpenStreetMapDiscoveryProvider osmProvider;
    private DuckDuckGoHtmlDiscoveryProvider webSearchProvider;
    private LeadDiscoveryService leadDiscoveryService;

    @BeforeEach
    void setUp() {
        osmProvider = new OpenStreetMapDiscoveryProvider(new ObjectMapper());
        webSearchProvider = new DuckDuckGoHtmlDiscoveryProvider();
        leadDiscoveryService = new LeadDiscoveryService(
                List.of(osmProvider, webSearchProvider),
                new WebScraperService()
        );
    }

    @Test
    @DisplayName("Verification 1: Salem + software company")
    void testSalemSoftwareCompany() {
        System.out.println("=== VERIFICATION 1: Salem + software company ===");
        LeadDiscoveryResponse response = leadDiscoveryService.discoverLeads(new LeadDiscoveryRequest("Salem", "software company", 5));
        assertNotNull(response);
        assertNotNull(response.getBusinesses());
        assertFalse(response.getBusinesses().isEmpty(), "Should discover software companies in Salem");
        System.out.println("Found " + response.getBusinesses().size() + " candidates for 'software company' in Salem:");
        for (DiscoveredBusinessDto b : response.getBusinesses()) {
            System.out.println(" - " + b.getBusinessName() + " | " + b.getWebsiteUrl() + " | src: " + b.getDiscoverySource());
        }
    }

    @Test
    @DisplayName("Verification 2: Chennai + colleges")
    void testChennaiColleges() {
        System.out.println("=== VERIFICATION 2: Chennai + colleges ===");
        LeadDiscoveryResponse response = leadDiscoveryService.discoverLeads(new LeadDiscoveryRequest("Chennai", "colleges", 5));
        assertNotNull(response);
        assertNotNull(response.getBusinesses());
        assertFalse(response.getBusinesses().isEmpty(), "Should discover colleges in Chennai");
        System.out.println("Found " + response.getBusinesses().size() + " candidates for 'colleges' in Chennai:");
        for (DiscoveredBusinessDto b : response.getBusinesses()) {
            System.out.println(" - " + b.getBusinessName() + " | " + b.getWebsiteUrl() + " | src: " + b.getDiscoverySource());
        }
    }

    @Test
    @DisplayName("Verification 3: Coimbatore + hospitals")
    void testCoimbatoreHospitals() {
        System.out.println("=== VERIFICATION 3: Coimbatore + hospitals ===");
        LeadDiscoveryResponse response = leadDiscoveryService.discoverLeads(new LeadDiscoveryRequest("Coimbatore", "hospitals", 5));
        assertNotNull(response);
        assertNotNull(response.getBusinesses());
        assertFalse(response.getBusinesses().isEmpty(), "Should discover hospitals in Coimbatore");
        System.out.println("Found " + response.getBusinesses().size() + " candidates for 'hospitals' in Coimbatore:");
        for (DiscoveredBusinessDto b : response.getBusinesses()) {
            System.out.println(" - " + b.getBusinessName() + " | " + b.getWebsiteUrl() + " | src: " + b.getDiscoverySource());
        }
    }

    @Test
    @DisplayName("Verification 4: Salem + restaurants")
    void testSalemRestaurants() {
        System.out.println("=== VERIFICATION 4: Salem + restaurants ===");
        LeadDiscoveryResponse response = leadDiscoveryService.discoverLeads(new LeadDiscoveryRequest("Salem", "restaurants", 5));
        assertNotNull(response);
        assertNotNull(response.getBusinesses());
        assertFalse(response.getBusinesses().isEmpty(), "Should discover restaurants in Salem");
        System.out.println("Found " + response.getBusinesses().size() + " candidates for 'restaurants' in Salem:");
        for (DiscoveredBusinessDto b : response.getBusinesses()) {
            System.out.println(" - " + b.getBusinessName() + " | " + b.getWebsiteUrl() + " | src: " + b.getDiscoverySource());
        }
    }

    @Test
    @DisplayName("Verification 5: Chennai + textile")
    void testChennaiTextile() {
        System.out.println("=== VERIFICATION 5: Chennai + textile ===");
        LeadDiscoveryResponse response = leadDiscoveryService.discoverLeads(new LeadDiscoveryRequest("Chennai", "textile", 5));
        assertNotNull(response);
        assertNotNull(response.getBusinesses());
        assertFalse(response.getBusinesses().isEmpty(), "Should discover textile businesses in Chennai");
        System.out.println("Found " + response.getBusinesses().size() + " candidates for 'textile' in Chennai:");
        for (DiscoveredBusinessDto b : response.getBusinesses()) {
            System.out.println(" - " + b.getBusinessName() + " | " + b.getWebsiteUrl() + " | src: " + b.getDiscoverySource());
        }
    }

    @Test
    @DisplayName("Verification 6: Custom / Unknown Keyword + Locality")
    void testCustomUnknownKeyword() {
        System.out.println("=== VERIFICATION 6: Fairlands, Salem + boutique ===");
        LeadDiscoveryResponse response = leadDiscoveryService.discoverLeads(new LeadDiscoveryRequest("Fairlands, Salem", "boutique", 5));
        assertNotNull(response);
        assertNotNull(response.getBusinesses());
        assertFalse(response.getBusinesses().isEmpty(), "Should discover candidates for custom keyword and locality");
        System.out.println("Found " + response.getBusinesses().size() + " candidates for 'boutique' in Fairlands, Salem:");
        for (DiscoveredBusinessDto b : response.getBusinesses()) {
            System.out.println(" - " + b.getBusinessName() + " | " + b.getWebsiteUrl() + " | src: " + b.getDiscoverySource());
        }
    }
}
