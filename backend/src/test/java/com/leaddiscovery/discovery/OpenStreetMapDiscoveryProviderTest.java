package com.leaddiscovery.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaddiscovery.dto.DiscoveredBusinessDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenStreetMapDiscoveryProviderTest {

    private OpenStreetMapDiscoveryProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OpenStreetMapDiscoveryProvider(new ObjectMapper());
    }

    @Test
    @DisplayName("Should return provider name correctly")
    void testGetProviderName() {
        assertEquals("OpenStreetMap-Nominatim", provider.getProviderName());
        assertTrue(provider.isEnabled());
    }

    @Test
    @DisplayName("Should return empty list for blank location or keyword")
    void testBlankInputs() {
        List<DiscoveredBusinessDto> results1 = provider.discover("", "Schools", 5);
        assertTrue(results1.isEmpty());

        List<DiscoveredBusinessDto> results2 = provider.discover("Salem", "", 5);
        assertTrue(results2.isEmpty());
    }

    @Test
    @DisplayName("Should discover real Schools in Salem category-agnostically")
    void testDiscoverSchools() {
        List<DiscoveredBusinessDto> results = provider.discover("Salem, Tamil Nadu", "Schools", 5);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should find school candidates in Salem");
        for (DiscoveredBusinessDto dto : results) {
            assertNotNull(dto.getBusinessName());
            assertFalse(dto.getBusinessName().isBlank());
            assertNotNull(dto.getWebsiteUrl());
            assertTrue(dto.getWebsiteUrl().startsWith("http"));
            assertEquals("OpenStreetMap-Nominatim", dto.getDiscoverySource());
        }
    }

    @Test
    @DisplayName("Should discover real Hospitals in Salem category-agnostically")
    void testDiscoverHospitals() {
        List<DiscoveredBusinessDto> results = provider.discover("Salem, Tamil Nadu", "Hospitals", 5);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should find hospital candidates in Salem");
        for (DiscoveredBusinessDto dto : results) {
            assertNotNull(dto.getBusinessName());
            assertFalse(dto.getBusinessName().isBlank());
            assertNotNull(dto.getWebsiteUrl());
            assertTrue(dto.getWebsiteUrl().startsWith("http"));
        }
    }

    @Test
    @DisplayName("Should discover real Hotels in Salem category-agnostically")
    void testDiscoverHotels() {
        List<DiscoveredBusinessDto> results = provider.discover("Salem, Tamil Nadu", "Hotels", 5);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should find hotel candidates in Salem");
        for (DiscoveredBusinessDto dto : results) {
            assertNotNull(dto.getBusinessName());
            assertFalse(dto.getBusinessName().isBlank());
            assertNotNull(dto.getWebsiteUrl());
            assertTrue(dto.getWebsiteUrl().startsWith("http"));
        }
    }

    @Test
    @DisplayName("Should discover Software Companies in Salem category-agnostically")
    void testDiscoverSoftwareCompanies() {
        List<DiscoveredBusinessDto> results = provider.discover("Salem, Tamil Nadu", "Software Companies", 5);
        assertNotNull(results);
        // Even if 0 or more are in OSM directly, the method returns cleanly without exception
        for (DiscoveredBusinessDto dto : results) {
            assertNotNull(dto.getBusinessName());
            assertFalse(dto.getBusinessName().isBlank());
            assertNotNull(dto.getWebsiteUrl());
            assertTrue(dto.getWebsiteUrl().startsWith("http"));
        }
    }
}
