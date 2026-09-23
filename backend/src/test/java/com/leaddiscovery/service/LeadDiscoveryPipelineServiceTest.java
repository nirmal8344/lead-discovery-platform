package com.leaddiscovery.service;

import com.leaddiscovery.dto.*;
import com.leaddiscovery.entity.*;
import com.leaddiscovery.entity.enums.SourcePageType;
import com.leaddiscovery.entity.enums.TaskStatus;
import com.leaddiscovery.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadDiscoveryPipelineServiceTest {

    @Mock private LeadDiscoveryService leadDiscoveryService;
    @Mock private WebScraperService webScraperService;
    @Mock private ScrapingTaskRepository taskRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private WebsiteRepository websiteRepository;
    @Mock private SourcePageRepository sourcePageRepository;
    @Mock private EmailAddressRepository emailAddressRepository;
    @Mock private PhoneNumberRepository phoneNumberRepository;
    @Mock private SocialLinkRepository socialLinkRepository;
    @Mock private ScrapingLogRepository logRepository;

    private ConfidenceScoringService confidenceScoringService;
    private LeadDiscoveryPipelineService pipelineService;

    @BeforeEach
    void setUp() {
        confidenceScoringService = new ConfidenceScoringService();
        pipelineService = new LeadDiscoveryPipelineService(
                leadDiscoveryService,
                webScraperService,
                confidenceScoringService,
                taskRepository,
                organizationRepository,
                websiteRepository,
                sourcePageRepository,
                emailAddressRepository,
                phoneNumberRepository,
                socialLinkRepository,
                logRepository
        );
    }

    @Test
    @DisplayName("Should successfully execute discovery, multi-page crawl, identity verification, and PostgreSQL persistence")
    void testExecutePipelineSuccess() {
        when(taskRepository.save(any(ScrapingTask.class))).thenAnswer(invocation -> {
            ScrapingTask t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(100L);
            return t;
        });

        List<DiscoveredBusinessDto> candidates = List.of(
                new DiscoveredBusinessDto("Apex Tech Solutions", "https://apextechsolutions.com", "src", "DuckDuckGo-HTML", 0.50, "CANDIDATE"),
                new DiscoveredBusinessDto("Salem Cloud", "https://salemcloud.in", "src", "DuckDuckGo-HTML", 0.50, "CANDIDATE")
        );
        when(leadDiscoveryService.discoverLeads(any(LeadDiscoveryRequest.class)))
                .thenReturn(new LeadDiscoveryResponse("Salem", "Software", 2, candidates, Collections.emptyList()));

        List<PageExtractDto> apexPages = List.of(
                new PageExtractDto("https://apextechsolutions.com", SourcePageType.HOME, "Home", 200,
                        Set.of(), Set.of(), Map.of(), Set.of(), LocalDateTime.now()),
                new PageExtractDto("https://apextechsolutions.com/contact", SourcePageType.CONTACT, "Contact Us", 200,
                        Set.of("info@apextechsolutions.com"), Set.of("+91 9876543210"),
                        Map.of("linkedin", "https://linkedin.com/company/apextech"),
                        Set.of("123 Tech Park, Salem"), LocalDateTime.now())
        );
        OrganizationIdentity apexIdentity = OrganizationIdentity.confirmed(
                "Apex Tech Solutions Pvt Ltd", "apextechsolutions.com", "JSON_LD_ORGANIZATION", 0.95
        );
        CrawlWebsiteResponse apexCrawl = new CrawlWebsiteResponse(
                "https://apextechsolutions.com", "apextechsolutions.com", 2, apexPages,
                Set.of("info@apextechsolutions.com"), Set.of("+91 9876543210"),
                Map.of("linkedin", "https://linkedin.com/company/apextech"),
                Set.of("123 Tech Park, Salem"), apexIdentity, LocalDateTime.now()
        );
        when(webScraperService.crawlWebsite(eq("https://apextechsolutions.com"), anyInt(), anyInt(), any()))
                .thenReturn(apexCrawl);

        List<PageExtractDto> cloudPages = List.of(
                new PageExtractDto("https://salemcloud.in", SourcePageType.HOME, "Home", 200,
                        Set.of("support@salemcloud.in"), Set.of("0427-2444555"), Map.of(), Set.of(), LocalDateTime.now())
        );
        OrganizationIdentity cloudIdentity = OrganizationIdentity.confirmed(
                "Salem Cloud Systems", "salemcloud.in", "OPENGRAPH_SITE_NAME", 0.90
        );
        CrawlWebsiteResponse cloudCrawl = new CrawlWebsiteResponse(
                "https://salemcloud.in", "salemcloud.in", 1, cloudPages,
                Set.of("support@salemcloud.in"), Set.of("04272444555"),
                Map.of(), Set.of(), cloudIdentity, LocalDateTime.now()
        );
        when(webScraperService.crawlWebsite(eq("https://salemcloud.in"), anyInt(), anyInt(), any()))
                .thenReturn(cloudCrawl);

        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> {
            Organization o = inv.getArgument(0);
            if (o.getId() == null) o.setId(200L);
            return o;
        });
        when(websiteRepository.save(any(Website.class))).thenAnswer(inv -> {
            Website w = inv.getArgument(0);
            if (w.getId() == null) w.setId(300L);
            return w;
        });
        when(sourcePageRepository.save(any(SourcePage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailAddressRepository.save(any(EmailAddress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phoneNumberRepository.save(any(PhoneNumber.class))).thenAnswer(inv -> inv.getArgument(0));
        when(socialLinkRepository.save(any(SocialLink.class))).thenAnswer(inv -> inv.getArgument(0));

        PipelineDiscoveryRequest request = new PipelineDiscoveryRequest("Salem", "Software", 5, 5);
        PipelineDiscoveryResponse response = pipelineService.executePipeline(request);

        assertNotNull(response);
        assertEquals(100L, response.getTaskId());
        assertEquals("Salem", response.getLocation());
        assertEquals("Software", response.getKeyword());
        assertEquals(2, response.getTotalDiscovered());
        assertEquals(2, response.getTotalCrawled());
        assertEquals(2, response.getTotalSaved());
        assertEquals(0, response.getTotalFailed());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(2, response.getLeads().size());

        // Verify verified company names were used rather than search-result titles
        assertEquals("Apex Tech Solutions Pvt Ltd", response.getLeads().get(0).getBusinessName());
        assertEquals("Salem Cloud Systems", response.getLeads().get(1).getBusinessName());

        verify(organizationRepository, times(2)).save(any(Organization.class));
        verify(websiteRepository, times(2)).save(any(Website.class));
    }

    @Test
    @DisplayName("Should reject and skip candidates when identity extraction identifies a listicle or directory")
    void testSkipRejectedCandidateIdentity() {
        when(taskRepository.save(any(ScrapingTask.class))).thenAnswer(invocation -> {
            ScrapingTask t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(102L);
            return t;
        });

        List<DiscoveredBusinessDto> candidates = List.of(
                new DiscoveredBusinessDto("Listicle Blog", "https://techblog.com/best-companies", "src", "DuckDuckGo-HTML", 0.50, "CANDIDATE")
        );
        when(leadDiscoveryService.discoverLeads(any(LeadDiscoveryRequest.class)))
                .thenReturn(new LeadDiscoveryResponse("Salem", "Software", 1, candidates, Collections.emptyList()));

        OrganizationIdentity rejectedIdentity = OrganizationIdentity.rejected("Candidate website is a listicle or directory roundup", "techblog.com");
        CrawlWebsiteResponse rejectedCrawl = new CrawlWebsiteResponse(
                "https://techblog.com/best-companies", "techblog.com", 1, List.of(),
                Set.of(), Set.of(), Map.of(), Set.of(), rejectedIdentity, LocalDateTime.now()
        );
        when(webScraperService.crawlWebsite(eq("https://techblog.com/best-companies"), anyInt(), anyInt(), any()))
                .thenReturn(rejectedCrawl);

        PipelineDiscoveryRequest request = new PipelineDiscoveryRequest("Salem", "Software", 5, 5);
        PipelineDiscoveryResponse response = pipelineService.executePipeline(request);

        assertNotNull(response);
        assertEquals(0, response.getTotalSaved());
        assertEquals(1, response.getTotalFailed());
        assertEquals("FAILED", response.getStatus());
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("Should handle partial failure gracefully when one website throws exception")
    void testExecutePipelineWithPartialFailure() {
        when(taskRepository.save(any(ScrapingTask.class))).thenAnswer(invocation -> {
            ScrapingTask t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(101L);
            return t;
        });

        List<DiscoveredBusinessDto> candidates = List.of(
                new DiscoveredBusinessDto("Good Company", "https://goodcompany.com", "src", "DuckDuckGo-HTML", 0.50, "CANDIDATE"),
                new DiscoveredBusinessDto("Broken Site", "https://broken-dead-site.com", "src", "DuckDuckGo-HTML", 0.50, "CANDIDATE")
        );
        when(leadDiscoveryService.discoverLeads(any(LeadDiscoveryRequest.class)))
                .thenReturn(new LeadDiscoveryResponse("Salem", "Software", 2, candidates, Collections.emptyList()));

        OrganizationIdentity goodIdentity = OrganizationIdentity.confirmed("Good Company Inc", "goodcompany.com", "HOMEPAGE_BRAND_TITLE", 0.85);
        CrawlWebsiteResponse goodCrawl = new CrawlWebsiteResponse(
                "https://goodcompany.com", "goodcompany.com", 1, List.of(),
                Set.of("info@goodcompany.com"), Set.of(), Map.of(), Set.of(), goodIdentity, LocalDateTime.now()
        );
        when(webScraperService.crawlWebsite(eq("https://goodcompany.com"), anyInt(), anyInt(), any())).thenReturn(goodCrawl);

        when(webScraperService.crawlWebsite(eq("https://broken-dead-site.com"), anyInt(), anyInt(), any()))
                .thenThrow(new RuntimeException("Connection timed out"));

        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> {
            Organization o = inv.getArgument(0);
            if (o.getId() == null) o.setId(201L);
            return o;
        });
        when(websiteRepository.save(any(Website.class))).thenAnswer(inv -> {
            Website w = inv.getArgument(0);
            if (w.getId() == null) w.setId(301L);
            return w;
        });

        PipelineDiscoveryRequest request = new PipelineDiscoveryRequest("Salem", "Software", 5, 5);
        PipelineDiscoveryResponse response = pipelineService.executePipeline(request);

        assertNotNull(response);
        assertEquals(2, response.getTotalDiscovered());
        assertEquals(1, response.getTotalCrawled());
        assertEquals(1, response.getTotalSaved());
        assertEquals(1, response.getTotalFailed());
        assertEquals(1, response.getLeads().size());
        assertEquals("COMPLETED", response.getStatus());
        assertTrue(response.getWarnings().stream().anyMatch(w -> w.contains("broken-dead-site.com")));
    }

    @Test
    @DisplayName("Should pass user requiredFields to crawler and scoring without crashing on missing fields")
    void testExecutePipelineWithCustomRequiredFields() {
        when(taskRepository.save(any(ScrapingTask.class))).thenAnswer(invocation -> {
            ScrapingTask t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(105L);
            return t;
        });

        List<DiscoveredBusinessDto> candidates = List.of(
                new DiscoveredBusinessDto("Targeted Corp", "https://targetedcorp.com", "src", "DuckDuckGo-HTML", 0.90, "CANDIDATE")
        );
        when(leadDiscoveryService.discoverLeads(any(LeadDiscoveryRequest.class)))
                .thenReturn(new LeadDiscoveryResponse("Salem", "Software", 1, candidates, Collections.emptyList()));

        List<PageExtractDto> pages = List.of(
                new PageExtractDto("https://targetedcorp.com", SourcePageType.HOME, "Targeted Corp Home", 200,
                        Set.of("contact@targetedcorp.com"), Set.of(), Map.of(), Set.of(), LocalDateTime.now())
        );
        OrganizationIdentity identity = OrganizationIdentity.confirmed("Targeted Corp", "targetedcorp.com", "JSON_LD_ORGANIZATION", 0.95);
        CrawlWebsiteResponse crawl = new CrawlWebsiteResponse(
                "https://targetedcorp.com", "targetedcorp.com", 1, pages,
                Set.of("contact@targetedcorp.com"), Set.of(), Set.of(), Map.of(), Set.of(), identity, LocalDateTime.now()
        );

        when(webScraperService.crawlWebsite(eq("https://targetedcorp.com"), eq(5), anyInt(), eq("EMAIL, PHONE, WHATSAPP")))
                .thenReturn(crawl);

        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> {
            Organization o = inv.getArgument(0);
            if (o.getId() == null) o.setId(205L);
            return o;
        });
        when(websiteRepository.save(any(Website.class))).thenAnswer(inv -> {
            Website w = inv.getArgument(0);
            if (w.getId() == null) w.setId(305L);
            return w;
        });

        PipelineDiscoveryRequest request = new PipelineDiscoveryRequest(
                "Salem", "Software", 5, 5, 25, "EMAIL, PHONE, WHATSAPP"
        );
        PipelineDiscoveryResponse response = pipelineService.executePipeline(request);

        assertNotNull(response);
        assertEquals(1, response.getTotalSaved());
        assertEquals("COMPLETED", response.getStatus());
        verify(webScraperService, times(1)).crawlWebsite(eq("https://targetedcorp.com"), eq(5), anyInt(), eq("EMAIL, PHONE, WHATSAPP"));
    }
}
