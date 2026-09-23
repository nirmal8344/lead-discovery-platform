package com.leaddiscovery.service;

import com.leaddiscovery.dto.OrganizationIdentity;
import com.leaddiscovery.dto.PageExtractDto;
import com.leaddiscovery.entity.enums.SourcePageType;
import com.leaddiscovery.entity.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConfidenceScoringServiceTest {

    private ConfidenceScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new ConfidenceScoringService();
    }

    @Test
    @DisplayName("Should award full points for verified organization identity and matching corporate email")
    void testHighConfidenceScore() {
        String websiteUrl = "https://innovativecloud.com";
        OrganizationIdentity identity = OrganizationIdentity.confirmed(
                "Innovative Cloud Technologies", "innovativecloud.com", "JSON_LD_ORGANIZATION", 0.95
        );
        List<PageExtractDto> pages = List.of(
                new PageExtractDto("https://innovativecloud.com", SourcePageType.HOME, "Home", 200,
                        Set.of(), Set.of(), Map.of(), Set.of(), LocalDateTime.now()),
                new PageExtractDto("https://innovativecloud.com/contact", SourcePageType.CONTACT, "Contact Us", 200,
                        Set.of("contact@innovativecloud.com"), Set.of("+15551234567"), Map.of(), Set.of("123 Cloud Way"), LocalDateTime.now())
        );
        Set<String> emails = Set.of("contact@innovativecloud.com");
        Set<String> phones = Set.of("+15551234567");
        Set<String> addresses = Set.of("123 Cloud Way, Tech City");
        Map<String, String> social = Map.of("linkedin", "https://linkedin.com/company/innovativecloud");

        ConfidenceScoringService.ConfidenceResult result = scoringService.calculateConfidence(
                identity, websiteUrl, pages, emails, phones, addresses, social
        );

        assertNotNull(result);
        // Domain(15) + Reachable(20) + Identity(25) + ContactPage(10) + Email(10) + DomainMatch(10) + Phone(5) + Address(5) + Social(5) = 100
        assertEquals(new BigDecimal("100.00"), result.getScore());
        assertEquals(VerificationStatus.VERIFIED, result.getStatus());
        assertTrue(result.getMissingFields().isEmpty());
    }

    @Test
    @DisplayName("Should apply penalty and prevent VERIFIED status when email domain mismatches website domain")
    void testEmailDomainMismatchPenalty() {
        String websiteUrl = "https://companya.com";
        OrganizationIdentity identity = OrganizationIdentity.confirmed(
                "Company A Inc", "companya.com", "JSON_LD_ORGANIZATION", 0.95
        );
        List<PageExtractDto> pages = List.of(
                new PageExtractDto("https://companya.com", SourcePageType.HOME, "Home", 200,
                        Set.of(), Set.of(), Map.of(), Set.of(), LocalDateTime.now())
        );
        // Email from completely different company domain
        Set<String> emails = Set.of("contact@unrelatedothercompany.com");
        Set<String> phones = Set.of("+15559876543");
        Set<String> addresses = Set.of();
        Map<String, String> social = Map.of();

        ConfidenceScoringService.ConfidenceResult result = scoringService.calculateConfidence(
                identity, websiteUrl, pages, emails, phones, addresses, social
        );

        assertNotNull(result);
        assertNotEquals(VerificationStatus.VERIFIED, result.getStatus());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("mismatch")));
    }

    @Test
    @DisplayName("Should classify UNVERIFIED when official domain is missing")
    void testMissingDomainConfidenceScore() {
        String websiteUrl = "";
        List<PageExtractDto> pages = List.of();
        Set<String> emails = Set.of("contact@randommail.com");
        Set<String> phones = Set.of("+919988776655");
        Set<String> addresses = Set.of();
        Map<String, String> social = Map.of();

        ConfidenceScoringService.ConfidenceResult result = scoringService.calculateConfidence(
                websiteUrl, pages, emails, phones, addresses, social
        );

        assertNotNull(result);
        assertEquals(VerificationStatus.UNVERIFIED, result.getStatus());
        assertTrue(result.getMissingFields().contains("website"));
    }

    @Test
    @DisplayName("Should normalize diverse user requiredFields inputs safely")
    void testParseAndNormalizeRequiredFields() {
        Set<String> fields1 = ConfidenceScoringService.parseAndNormalizeRequiredFields("EMAIL, PHONE, SOCIAL");
        assertTrue(fields1.contains("email"));
        assertTrue(fields1.contains("phone"));
        assertTrue(fields1.contains("social_links"));

        Set<String> fields2 = ConfidenceScoringService.parseAndNormalizeRequiredFields("Company Name, Pincode, WhatsApp, Contact Person");
        assertTrue(fields2.contains("company_name"));
        assertTrue(fields2.contains("pincode"));
        assertTrue(fields2.contains("whatsapp"));
        assertTrue(fields2.contains("contact_person"));

        Set<String> emptyFields = ConfidenceScoringService.parseAndNormalizeRequiredFields(null);
        assertTrue(emptyFields.isEmpty());
    }

    @Test
    @DisplayName("Should only flag missing fields that were explicitly requested by user")
    void testSelectiveRequiredFieldsMissingEvaluation() {
        String websiteUrl = "https://mybusiness.com";
        List<PageExtractDto> pages = List.of(
                new PageExtractDto("https://mybusiness.com", SourcePageType.HOME, "Home", 200,
                        Set.of("hi@mybusiness.com"), Set.of(), Map.of(), Set.of(), LocalDateTime.now())
        );
        Set<String> emails = Set.of("hi@mybusiness.com");
        Set<String> phones = Set.of();
        Set<String> addresses = Set.of();
        Map<String, String> social = Map.of();

        // User only requires EMAIL
        ConfidenceScoringService.ConfidenceResult resultEmailOnly = scoringService.calculateConfidence(
                null, websiteUrl, pages, emails, phones, addresses, social, "EMAIL"
        );
        assertNotNull(resultEmailOnly);
        // Address, Phone, Social should NOT be marked missing because user only asked for EMAIL
        assertFalse(resultEmailOnly.getMissingFields().contains("address"));
        assertFalse(resultEmailOnly.getMissingFields().contains("phone"));
        assertFalse(resultEmailOnly.getMissingFields().contains("social_links"));

        // User requires EMAIL and PHONE
        ConfidenceScoringService.ConfidenceResult resultEmailAndPhone = scoringService.calculateConfidence(
                null, websiteUrl, pages, emails, phones, addresses, social, "EMAIL, PHONE"
        );
        assertTrue(resultEmailAndPhone.getMissingFields().contains("phone"));
        assertFalse(resultEmailAndPhone.getMissingFields().contains("address"));
        assertFalse(resultEmailAndPhone.getMissingFields().contains("whatsapp"));

        // User requires EMAIL + PHONE + WHATSAPP
        ConfidenceScoringService.ConfidenceResult resultWithWhatsApp = scoringService.calculateConfidence(
                null, websiteUrl, pages, emails, phones, Set.of("+919876543210"), addresses, social, "EMAIL, PHONE, WHATSAPP"
        );
        assertFalse(resultWithWhatsApp.getMissingFields().contains("whatsapp"), "WhatsApp present should not be missing");
        assertTrue(resultWithWhatsApp.getMissingFields().contains("phone"), "Missing phone should still be reported");

        // User requires EMAIL + PHONE + WHATSAPP but WhatsApp is missing
        ConfidenceScoringService.ConfidenceResult resultMissingWhatsApp = scoringService.calculateConfidence(
                null, websiteUrl, pages, emails, phones, Set.of(), addresses, social, "EMAIL, PHONE, WHATSAPP"
        );
        assertTrue(resultMissingWhatsApp.getMissingFields().contains("whatsapp"), "Missing requested WhatsApp should be reported");
    }
}
