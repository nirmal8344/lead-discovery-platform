package com.leaddiscovery.service;

import com.leaddiscovery.dto.LeadDetailDto;
import com.leaddiscovery.dto.LeadFilterCriteria;
import com.leaddiscovery.dto.LeadListItemDto;
import com.leaddiscovery.entity.*;
import com.leaddiscovery.entity.enums.PhoneType;
import com.leaddiscovery.entity.enums.Role;
import com.leaddiscovery.entity.enums.VerificationStatus;
import com.leaddiscovery.entity.enums.WebsiteStatus;
import com.leaddiscovery.exception.ResourceNotFoundException;
import com.leaddiscovery.repository.OrganizationRepository;
import com.leaddiscovery.repository.ScrapingTaskRepository;
import com.leaddiscovery.security.SecurityUtils;
import com.leaddiscovery.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ScrapingTaskRepository taskRepository;

    @Mock
    private SecurityUtils securityUtils;

    private LeadService leadService;
    private User testUser;
    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        leadService = new LeadService(organizationRepository, taskRepository, securityUtils);

        testUser = new User("John Doe", "john@example.com", "hashed_pwd", Role.USER);
        testUser.setId(100L);
        testUserPrincipal = new UserPrincipal(100L, "John Doe", "john@example.com", "hashed_pwd", Role.USER, Collections.emptyList());
    }

    @Test
    @DisplayName("Should list leads with pagination and filter criteria scoped to user")
    void testListLeads() {
        Organization org = new Organization();
        org.setId(1L);
        org.setBusinessName("Test Corp");
        org.setCity("Salem");
        org.setCategory("Software");
        org.setVerificationStatus(VerificationStatus.VERIFIED);
        org.setConfidenceScore(new BigDecimal("90.00"));

        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);
        when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(org)));

        LeadFilterCriteria criteria = new LeadFilterCriteria("Salem", "Software", VerificationStatus.VERIFIED, "Test", null);
        Page<LeadListItemDto> result = leadService.listLeads(criteria, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Corp", result.getContent().get(0).getBusinessName());
        assertEquals(100L, criteria.getUserId());
    }

    @Test
    @DisplayName("Should return full lead details when user owns the task")
    void testGetLeadDetailsSuccess() {
        ScrapingTask task = new ScrapingTask();
        task.setId(55L);
        task.setCreatedBy(testUser);

        Organization org = new Organization();
        org.setId(1L);
        org.setScrapingTask(task);
        org.setBusinessName("Apex Tech");
        org.setNormalizedName("apex tech");
        org.setCity("Salem");
        org.setCategory("IT Services");
        org.setConfidenceScore(new BigDecimal("95.00"));
        org.setVerificationStatus(VerificationStatus.VERIFIED);

        Website site = new Website();
        site.setId(10L);
        site.setUrl("https://apextech.com");
        site.setNormalizedUrl("https://apextech.com");
        site.setStatus(WebsiteStatus.CRAWLED);
        site.setIsOfficial(true);
        org.getWebsites().add(site);

        EmailAddress email = new EmailAddress();
        email.setId(20L);
        email.setRawValue("info@apextech.com");
        email.setNormalizedValue("info@apextech.com");
        org.getEmailAddresses().add(email);

        PhoneNumber phone = new PhoneNumber();
        phone.setId(30L);
        phone.setRawValue("+919876543210");
        phone.setNormalizedValue("+919876543210");
        phone.setPhoneType(PhoneType.PHONE);
        org.getPhoneNumbers().add(phone);

        SocialLink social = new SocialLink();
        social.setId(40L);
        social.setPlatform("linkedin");
        social.setUrl("https://linkedin.com/company/apextech");
        org.getSocialLinks().add(social);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);

        LeadDetailDto detail = leadService.getLeadDetails(1L);

        assertNotNull(detail);
        assertEquals(1L, detail.getId());
        assertEquals(55L, detail.getTaskId());
        assertEquals("Apex Tech", detail.getBusinessName());
        assertEquals(1, detail.getWebsites().size());
        assertEquals(1, detail.getEmailAddresses().size());
        assertEquals(1, detail.getPhoneNumbers().size());
        assertEquals(1, detail.getSocialLinks().size());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when accessing lead owned by another user (IDOR)")
    void testGetLeadDetailsOtherUser() {
        User otherUser = new User("Other", "other@example.com", "pwd", Role.USER);
        otherUser.setId(200L);

        ScrapingTask task = new ScrapingTask();
        task.setId(55L);
        task.setCreatedBy(otherUser);

        Organization org = new Organization();
        org.setId(1L);
        org.setScrapingTask(task);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);

        assertThrows(AccessDeniedException.class, () -> leadService.getLeadDetails(1L));
    }

    @Test
    @DisplayName("Should delete lead when owned by user")
    void testDeleteLeadSuccess() {
        ScrapingTask task = new ScrapingTask();
        task.setCreatedBy(testUser);

        Organization org = new Organization();
        org.setId(1L);
        org.setScrapingTask(task);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);

        leadService.deleteLead(1L);

        verify(organizationRepository, times(1)).delete(org);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when deleting lead owned by another user")
    void testDeleteLeadOtherUser() {
        User otherUser = new User("Other", "other@example.com", "pwd", Role.USER);
        otherUser.setId(200L);

        ScrapingTask task = new ScrapingTask();
        task.setCreatedBy(otherUser);

        Organization org = new Organization();
        org.setId(1L);
        org.setScrapingTask(task);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);

        assertThrows(AccessDeniedException.class, () -> leadService.deleteLead(1L));
        verify(organizationRepository, never()).delete(any(Organization.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent lead")
    void testDeleteLeadNotFound() {
        when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leadService.deleteLead(999L));
        verify(organizationRepository, never()).delete(any(Organization.class));
    }

    @Test
    @DisplayName("Should cleanly disassociate sourcePage and delete lead with all child records")
    void testDeleteLeadWithAssociatedChildren() {
        ScrapingTask task = new ScrapingTask();
        task.setCreatedBy(testUser);

        Organization org = new Organization();
        org.setId(5L);
        org.setScrapingTask(task);

        SourcePage sp = new SourcePage();
        sp.setId(101L);

        EmailAddress email = new EmailAddress();
        email.setSourcePage(sp);
        org.getEmailAddresses().add(email);

        PhoneNumber phone = new PhoneNumber();
        phone.setSourcePage(sp);
        org.getPhoneNumbers().add(phone);

        SocialLink social = new SocialLink();
        social.setSourcePage(sp);
        org.getSocialLinks().add(social);

        Contact contact = new Contact();
        contact.setSourcePage(sp);
        org.getContacts().add(contact);

        when(organizationRepository.findById(5L)).thenReturn(Optional.of(org));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);

        leadService.deleteLead(5L);

        assertNull(email.getSourcePage());
        assertNull(phone.getSourcePage());
        assertNull(social.getSourcePage());
        assertNull(contact.getSourcePage());
        verify(organizationRepository, times(1)).delete(org);
        verify(organizationRepository, times(1)).flush();
    }

    @Test
    @DisplayName("Should return leads for a specific task ID when task owned by user")
    void testListLeadsByTask() {
        ScrapingTask task = new ScrapingTask();
        task.setId(10L);
        task.setCreatedBy(testUser);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);

        Organization org = new Organization();
        org.setId(1L);
        org.setBusinessName("Task Specific Lead");

        when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(org)));

        Page<LeadListItemDto> leads = leadService.listLeadsByTask(10L, PageRequest.of(0, 10));

        assertNotNull(leads);
        assertEquals(1, leads.getTotalElements());
        assertEquals("Task Specific Lead", leads.getContent().get(0).getBusinessName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when querying leads for non-existent task ID")
    void testListLeadsByTaskNotFound() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leadService.listLeadsByTask(999L, PageRequest.of(0, 10)));
    }
}
