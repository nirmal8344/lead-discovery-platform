package com.leaddiscovery.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaddiscovery.dto.DiscoveredBusinessDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DiscoveryVerificationTest {

    private OpenStreetMapDiscoveryProvider osmProvider;

    @BeforeEach
    void setUp() {
        osmProvider = new OpenStreetMapDiscoveryProvider(new ObjectMapper());
    }

    @Test
    @DisplayName("Verification 1: schools in Chennai")
    void testSchoolsInChennai() {
        System.out.println("=== VERIFICATION 1: schools in Chennai ===");
        List<DiscoveredBusinessDto> results = osmProvider.discover("Chennai", "schools", 10);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should discover schools in Chennai");
        System.out.println("Found " + results.size() + " candidates for 'schools' in Chennai:");
        for (DiscoveredBusinessDto b : results) {
            System.out.println(" - " + b.getBusinessName() + " | " + b.getWebsiteUrl() + " | dist: " + b.getDistanceKm() + "km");
        }
    }

    @Test
    @DisplayName("Verification 2: software company in Chennai")
    void testSoftwareCompanyInChennai() {
        System.out.println("=== VERIFICATION 2: software company in Chennai ===");
        List<DiscoveredBusinessDto> results = osmProvider.discover("Chennai", "software company", 10);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should discover software companies in Chennai");
        System.out.println("Found " + results.size() + " candidates for 'software company' in Chennai:");
        for (DiscoveredBusinessDto b : results) {
            System.out.println(" - " + b.getBusinessName() + " | " + b.getWebsiteUrl() + " | dist: " + b.getDistanceKm() + "km");
        }
    }

    @Test
    @DisplayName("Verification 3: schools in Salem")
    void testSchoolsInSalem() {
        System.out.println("=== VERIFICATION 3: schools in Salem ===");
        List<DiscoveredBusinessDto> results = osmProvider.discover("Salem", "schools", 10);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should discover schools in Salem");
        System.out.println("Found " + results.size() + " candidates for 'schools' in Salem:");
        for (DiscoveredBusinessDto b : results) {
            System.out.println(" - " + b.getBusinessName() + " | " + b.getWebsiteUrl() + " | dist: " + b.getDistanceKm() + "km");
        }
    }
}
