package com.leaddiscovery.service;

import com.leaddiscovery.discovery.UrlFilterUtils;
import com.leaddiscovery.dto.LeadDetailDto;
import com.leaddiscovery.dto.LeadListItemDto;
import com.leaddiscovery.dto.OrganizationIdentity;
import com.leaddiscovery.entity.*;
import com.leaddiscovery.entity.enums.ContactVerificationStatus;
import com.leaddiscovery.entity.enums.IdentityValidationResult;
import com.leaddiscovery.entity.enums.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContactProvenanceTest {

    @Test
    @DisplayName("Listicle and article URLs or titles must be detected and rejected")
    void testListicleRejection() {
        assertTrue(UrlFilterUtils.isListicleOrDirectory(
                "Top 15 IT Companies in Salem (2026)",
                "https://brandveda.in/blog/it-companies-in-salem"
        ));

        assertTrue(UrlFilterUtils.isListicleOrDirectory(
                "Best Software Companies in Salem",
                "https://techdirectory.com/companies-in/salem"
        ));

        assertTrue(UrlFilterUtils.isListicleOrDirectory(
                "10 Best IT Solutions Providers",
                "https://example.com/blogs/best-it-solutions"
        ));

        // Legitimate company homepage
        assertFalse(UrlFilterUtils.isListicleOrDirectory(
                "Apex Tech Solutions - Cloud & AI Consulting",
                "https://apextechsolutions.com"
        ));
    }

    @Test
    @DisplayName("Email domain matching should verify matching domain and mark third-party as EXTERNAL")
    void testEmailDomainMatching() {
        String officialDomain = "abctechnologies.com";

        // 1. Direct official domain email -> VERIFIED
        assertEquals(ContactVerificationStatus.VERIFIED,
                ConfidenceScoringService.evaluateEmailVerification("sales@abctechnologies.com", officialDomain));
        assertEquals(ContactVerificationStatus.VERIFIED,
                ConfidenceScoringService.evaluateEmailVerification("support@sub.abctechnologies.com", officialDomain));

        // 2. Free email provider -> UNVERIFIED
        assertEquals(ContactVerificationStatus.UNVERIFIED,
                ConfidenceScoringService.evaluateEmailVerification("someone@gmail.com", officialDomain));
        assertEquals(ContactVerificationStatus.UNVERIFIED,
                ConfidenceScoringService.evaluateEmailVerification("contact@yahoo.com", officialDomain));

        // 3. Third-party corporate domain -> EXTERNAL (e.g. Capgemini email on an article or another site)
        assertEquals(ContactVerificationStatus.EXTERNAL,
                ConfidenceScoringService.evaluateEmailVerification("cgcompanysecretary.in@capgemini.com", officialDomain));
        assertEquals(ContactVerificationStatus.EXTERNAL,
                ConfidenceScoringService.evaluateEmailVerification("info@appexive.com", officialDomain));
        assertEquals(ContactVerificationStatus.EXTERNAL,
                ConfidenceScoringService.evaluateEmailVerification("info@foxthreetechnologies.com", officialDomain));
    }

    @Test
    @DisplayName("LeadDetailDto fromEntity preserves contact provenance and official domain")
    void testLeadDetailDtoProvenance() {
        Organization org = new Organization();
        org.setId(101L);
        org.setBusinessName("ABC Technologies");
        org.setNormalizedName("abc technologies");
        org.setCity("Salem");
        org.setCategory("Software Development");
        org.setVerificationStatus(VerificationStatus.VERIFIED);
        org.setConfidenceScore(new BigDecimal("90.00"));
        org.setCreatedAt(LocalDateTime.now());

        Website website = new Website();
        website.setId(1L);
        website.setUrl("https://abctechnologies.com");
        website.setNormalizedUrl("https://abctechnologies.com");
        website.setIsOfficial(true);
        website.setOrganization(org);
        org.setWebsites(List.of(website));

        SourcePage contactPage = new SourcePage();
        contactPage.setId(5L);
        contactPage.setPageUrl("https://abctechnologies.com/contact");
        contactPage.setWebsite(website);

        EmailAddress verifiedEmail = new EmailAddress();
        verifiedEmail.setId(201L);
        verifiedEmail.setRawValue("sales@abctechnologies.com");
        verifiedEmail.setNormalizedValue("sales@abctechnologies.com");
        verifiedEmail.setSourcePage(contactPage);
        verifiedEmail.setSourceDomain("abctechnologies.com");
        verifiedEmail.setVerificationStatus(ContactVerificationStatus.VERIFIED);
        verifiedEmail.setOrganization(org);

        EmailAddress externalEmail = new EmailAddress();
        externalEmail.setId(202L);
        externalEmail.setRawValue("info@thirdparty.com");
        externalEmail.setNormalizedValue("info@thirdparty.com");
        externalEmail.setSourcePage(contactPage);
        externalEmail.setSourceDomain("thirdparty.com");
        externalEmail.setVerificationStatus(ContactVerificationStatus.EXTERNAL);
        externalEmail.setOrganization(org);

        org.setEmailAddresses(List.of(verifiedEmail, externalEmail));

        PhoneNumber phone = new PhoneNumber();
        phone.setId(301L);
        phone.setRawValue("+91 98765 43210");
        phone.setNormalizedValue("+919876543210");
        phone.setSourcePage(contactPage);
        phone.setSourceDomain("abctechnologies.com");
        phone.setVerificationStatus(ContactVerificationStatus.VERIFIED);
        phone.setOrganization(org);
        org.setPhoneNumbers(List.of(phone));

        LeadDetailDto dto = LeadDetailDto.fromEntity(org);

        assertEquals("ABC Technologies", dto.getBusinessName());
        assertEquals("https://abctechnologies.com", dto.getOfficialDomain());
        assertEquals(2, dto.getEmailAddresses().size());

        LeadDetailDto.EmailAddressDto emailDto1 = dto.getEmailAddresses().get(0);
        assertEquals("sales@abctechnologies.com", emailDto1.getNormalizedValue());
        assertEquals("https://abctechnologies.com/contact", emailDto1.getSourcePageUrl());
        assertEquals("abctechnologies.com", emailDto1.getSourceDomain());
        assertEquals("VERIFIED", emailDto1.getVerificationStatus());

        LeadDetailDto.EmailAddressDto emailDto2 = dto.getEmailAddresses().get(1);
        assertEquals("info@thirdparty.com", emailDto2.getNormalizedValue());
        assertEquals("EXTERNAL", emailDto2.getVerificationStatus());

        assertEquals(1, dto.getPhoneNumbers().size());
        LeadDetailDto.PhoneNumberDto phoneDto = dto.getPhoneNumbers().get(0);
        assertEquals("+919876543210", phoneDto.getNormalizedValue());
        assertEquals("https://abctechnologies.com/contact", phoneDto.getSourcePageUrl());
        assertEquals("VERIFIED", phoneDto.getVerificationStatus());
    }

    @Test
    @DisplayName("LeadListItemDto prioritizes verified corporate contacts for table view")
    void testLeadListItemDtoVerifiedPrioritization() {
        Organization org = new Organization();
        org.setId(102L);
        org.setBusinessName("ABC Technologies");
        org.setCity("Salem");
        org.setVerificationStatus(VerificationStatus.VERIFIED);
        org.setConfidenceScore(new BigDecimal("85.00"));

        Website website = new Website();
        website.setUrl("https://abctechnologies.com");
        website.setIsOfficial(true);
        website.setOrganization(org);
        org.setWebsites(List.of(website));

        // Add external email first in list, then verified email
        EmailAddress externalEmail = new EmailAddress();
        externalEmail.setNormalizedValue("info@unrelated.com");
        externalEmail.setVerificationStatus(ContactVerificationStatus.EXTERNAL);

        EmailAddress verifiedEmail = new EmailAddress();
        verifiedEmail.setNormalizedValue("contact@abctechnologies.com");
        verifiedEmail.setVerificationStatus(ContactVerificationStatus.VERIFIED);

        org.setEmailAddresses(List.of(externalEmail, verifiedEmail));

        LeadListItemDto item = LeadListItemDto.fromEntity(org);
        // Must select the VERIFIED email instead of the external first one
        assertEquals("contact@abctechnologies.com", item.getPrimaryEmail());
        assertEquals("https://abctechnologies.com", item.getWebsiteUrl());
    }
}
