package com.leaddiscovery.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataNormalizationUtilsTest {

    @Test
    @DisplayName("Should normalize valid emails and reject invalid ones")
    void testNormalizeEmail() {
        assertEquals("info@acme.com", DataNormalizationUtils.normalizeEmail("  Info@Acme.COM "));
        assertEquals("sales.dept@sub.domain.co.in", DataNormalizationUtils.normalizeEmail("sales.dept@sub.domain.co.in"));
        assertNull(DataNormalizationUtils.normalizeEmail("not-an-email"));
        assertNull(DataNormalizationUtils.normalizeEmail("image@domain.png"));
        assertNull(DataNormalizationUtils.normalizeEmail(""));
        assertNull(DataNormalizationUtils.normalizeEmail(null));
    }

    @Test
    @DisplayName("Should normalize phone numbers and preserve leading plus")
    void testNormalizePhoneNumber() {
        assertEquals("+919876543210", DataNormalizationUtils.normalizePhoneNumber("+91 (987) 654-3210"));
        assertEquals("+15552345678", DataNormalizationUtils.normalizePhoneNumber("+1-555-234-5678"));
        assertEquals("9876543210", DataNormalizationUtils.normalizePhoneNumber("09876543210".substring(1)));
        assertNull(DataNormalizationUtils.normalizePhoneNumber("123")); // Too short
        assertNull(DataNormalizationUtils.normalizePhoneNumber("1111111111")); // Repeated single digit
        assertNull(DataNormalizationUtils.normalizePhoneNumber(null));
    }

    @Test
    @DisplayName("Should normalize business names and strip common suffixes")
    void testNormalizeBusinessName() {
        assertEquals("apex tech solutions", DataNormalizationUtils.normalizeBusinessName("Apex Tech Solutions Pvt Ltd"));
        assertEquals("apex tech solutions", DataNormalizationUtils.normalizeBusinessName("Apex Tech Solutions - Home"));
        assertEquals("apex tech solutions", DataNormalizationUtils.normalizeBusinessName("Apex Tech Solutions, Inc."));
        assertEquals("apex tech solutions", DataNormalizationUtils.normalizeBusinessName("Apex Tech Solutions | Official Site"));
    }

    @Test
    @DisplayName("Should normalize physical addresses and collapse whitespace")
    void testNormalizeAddress() {
        String raw = "  123 Main Street,\nSuite 400,\tSpringfield, IL   62701  ";
        assertEquals("123 Main Street, Suite 400, Springfield, IL 62701", DataNormalizationUtils.normalizeAddress(raw));
        assertNull(DataNormalizationUtils.normalizeAddress(""));
        assertNull(DataNormalizationUtils.normalizeAddress("abc")); // Too short
    }
}
