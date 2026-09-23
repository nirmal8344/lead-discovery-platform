package com.leaddiscovery.service;

import com.leaddiscovery.dto.LeadFilterCriteria;
import com.leaddiscovery.entity.Contact;
import com.leaddiscovery.entity.Organization;
import com.leaddiscovery.entity.enums.Role;
import com.leaddiscovery.entity.enums.VerificationStatus;
import com.leaddiscovery.repository.OrganizationRepository;
import com.leaddiscovery.security.SecurityUtils;
import com.leaddiscovery.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadExportServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SecurityUtils securityUtils;

    private LeadExportService exportService;
    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        exportService = new LeadExportService(organizationRepository, securityUtils);
        testUserPrincipal = new UserPrincipal(100L, "John Doe", "john@example.com", "pwd", Role.USER, Collections.emptyList());
    }

    @Test
    @DisplayName("Should prevent CSV formula injection on cells starting with dangerous symbols")
    void testFormulaInjectionProtection() {
        assertEquals("'=SUM(A1:A10)", LeadExportService.sanitizeFormulaInjection("=SUM(A1:A10)"));
        assertEquals("'+12345", LeadExportService.sanitizeFormulaInjection("+12345"));
        assertEquals("'-500", LeadExportService.sanitizeFormulaInjection("-500"));
        assertEquals("'@cmd", LeadExportService.sanitizeFormulaInjection("@cmd"));
        assertEquals("Normal Company", LeadExportService.sanitizeFormulaInjection("Normal Company"));
    }

    @Test
    @DisplayName("Should escape commas and quotes in CSV cells correctly")
    void testCsvCellEscaping() {
        assertEquals("\"Acme, Inc.\"", LeadExportService.escapeCsvCell("Acme, Inc."));
        assertEquals("\"Acme \"\"Best\"\" Corp\"", LeadExportService.escapeCsvCell("Acme \"Best\" Corp"));
        assertEquals("\"Line 1\nLine 2\"", LeadExportService.escapeCsvCell("Line 1\nLine 2"));
    }

    @Test
    @DisplayName("Should export leads to CSV with structured headers and formatted data scoped to user")
    void testExportLeadsToCsv() throws IOException {
        Organization org = new Organization();
        org.setId(10L);
        org.setBusinessName("Apex Tech Solutions");
        org.setCity("Salem");
        org.setState("Tamil Nadu");
        org.setCountry("India");
        org.setCategory("Software Companies");
        org.setAddress("123 Tech Park, Salem");
        org.setConfidenceScore(new BigDecimal("95.00"));
        org.setVerificationStatus(VerificationStatus.VERIFIED);
        org.setSourceUrl("https://search.source/test");
        org.setCreatedAt(LocalDateTime.of(2026, 9, 19, 10, 0));

        Contact contact = new Contact();
        contact.setContactPerson("Sundar Pitchai");
        contact.setRole("Managing Director");
        contact.setOrganization(org);
        org.getContacts().add(contact);

        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);
        when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(org)));

        StringWriter writer = new StringWriter();
        LeadFilterCriteria criteria = new LeadFilterCriteria();
        exportService.exportLeadsToCsv(criteria, writer);

        String csvOutput = writer.toString();
        assertNotNull(csvOutput);
        assertTrue(csvOutput.startsWith("\"Lead ID\",\"Company Name\",\"Category\",\"Contact Person\",\"Contact Role\",\"Location\",\"City\",\"State\",\"Country\",\"Address\",\"Official Website\",\"Email\",\"Email Source URL\",\"Email Source Domain\",\"Email Verification Status\",\"Phone\",\"Phone Source URL\",\"WhatsApp\",\"WhatsApp Source URL\",\"Social Links\",\"Confidence\",\"Verification Status\",\"Discovery Source URL\",\"Created At\""));
        assertTrue(csvOutput.contains("\"10\",\"Apex Tech Solutions\",\"Software Companies\",\"Sundar Pitchai\",\"Managing Director\",\"Salem, Tamil Nadu, India\",\"Salem\",\"Tamil Nadu\",\"India\""));
        assertTrue(csvOutput.contains("\"95.00\",\"VERIFIED\""));
        assertEquals(100L, criteria.getUserId());
    }
}

