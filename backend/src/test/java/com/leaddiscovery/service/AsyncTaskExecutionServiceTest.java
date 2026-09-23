package com.leaddiscovery.service;

import com.leaddiscovery.dto.CrawlWebsiteResponse;
import com.leaddiscovery.dto.DiscoveredBusinessDto;
import com.leaddiscovery.dto.LeadDiscoveryRequest;
import com.leaddiscovery.dto.LeadDiscoveryResponse;
import com.leaddiscovery.entity.ScrapingTask;
import com.leaddiscovery.entity.enums.TaskStatus;
import com.leaddiscovery.repository.ScrapingLogRepository;
import com.leaddiscovery.repository.ScrapingTaskRepository;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncTaskExecutionServiceTest {

    @Mock private ScrapingTaskRepository taskRepository;
    @Mock private ScrapingLogRepository logRepository;
    @Mock private LeadDiscoveryService leadDiscoveryService;
    @Mock private WebScraperService webScraperService;
    @Mock private LeadDiscoveryPipelineService pipelineService;

    private AsyncTaskExecutionService asyncService;

    @BeforeEach
    void setUp() {
        lenient().when(taskRepository.save(any(ScrapingTask.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(taskRepository.saveAndFlush(any(ScrapingTask.class))).thenAnswer(inv -> inv.getArgument(0));
        asyncService = new AsyncTaskExecutionService(
                taskRepository, logRepository, leadDiscoveryService, webScraperService, pipelineService
        );
    }

    @Test
    @DisplayName("Should execute background task and transition status to COMPLETED")
    void testRunTaskAsyncSuccess() {
        ScrapingTask task = new ScrapingTask();
        task.setId(10L);
        task.setLocation("Salem");
        task.setKeyword("Software");
        task.setMaxResults(5);
        task.setMaxPagesPerSite(3);
        task.setStatus(TaskStatus.QUEUED);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        List<DiscoveredBusinessDto> candidates = List.of(
                new DiscoveredBusinessDto("Company A", "https://companya.com", "src", "DuckDuckGo-HTML", 0.9, "DISCOVERED")
        );
        when(leadDiscoveryService.discoverLeads(any(LeadDiscoveryRequest.class)))
                .thenReturn(new LeadDiscoveryResponse("Salem", "Software", 1, candidates, Collections.emptyList()));

        CrawlWebsiteResponse crawl = new CrawlWebsiteResponse(
                "https://companya.com", "companya.com", 1, List.of(),
                Set.of("info@companya.com"), Set.of("+919876543210"), Map.of(), Set.of(), LocalDateTime.now()
        );
        when(webScraperService.crawlWebsite(eq("https://companya.com"), eq(3), anyInt(), any())).thenReturn(crawl);

        asyncService.runTaskAsync(10L);

        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertEquals(100, task.getProgressPercentage());
        assertEquals(1, task.getDiscoveredBusinesses());
        assertEquals(1, task.getLeadsSaved());
        assertNotNull(task.getStartTime());
        assertNotNull(task.getEndTime());
        verify(pipelineService, times(1)).persistDiscoveredLead(eq(task), any(), any());
    }

    @Test
    @DisplayName("Should handle pre-start cancellation signal cleanly")
    void testRunTaskAsyncPreStartCancellation() {
        ScrapingTask task = new ScrapingTask();
        task.setId(20L);
        task.setLocation("Salem");
        task.setKeyword("Software");
        task.setMaxResults(5);
        task.setMaxPagesPerSite(3);
        task.setStatus(TaskStatus.QUEUED);

        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));

        // Signal cancellation before execution begins
        asyncService.requestCancellation(20L);

        asyncService.runTaskAsync(20L);

        assertEquals(TaskStatus.CANCELLED, task.getStatus());
        assertEquals("CANCELLED", task.getCurrentStage());
        assertNotNull(task.getEndTime());
        verify(leadDiscoveryService, never()).discoverLeads(any());
    }

    @Test
    @DisplayName("Should handle mid-execution cooperative cancellation cleanly")
    void testRunTaskAsyncMidExecutionCancellation() {
        ScrapingTask task = new ScrapingTask();
        task.setId(21L);
        task.setLocation("Salem");
        task.setKeyword("Software");
        task.setMaxResults(5);
        task.setMaxPagesPerSite(3);
        task.setStatus(TaskStatus.QUEUED);

        when(taskRepository.findById(21L)).thenReturn(Optional.of(task));

        List<DiscoveredBusinessDto> candidates = List.of(
                new DiscoveredBusinessDto("Company A", "https://companya.com", "src", "DuckDuckGo-HTML", 0.9, "DISCOVERED"),
                new DiscoveredBusinessDto("Company B", "https://companyb.com", "src", "DuckDuckGo-HTML", 0.9, "DISCOVERED")
        );
        when(leadDiscoveryService.discoverLeads(any(LeadDiscoveryRequest.class)))
                .thenReturn(new LeadDiscoveryResponse("Salem", "Software", 2, candidates, Collections.emptyList()));

        // When Company A is crawled, trigger cancellation for task 21
        when(webScraperService.crawlWebsite(eq("https://companya.com"), anyInt(), anyInt(), any())).thenAnswer(inv -> {
            asyncService.requestCancellation(21L);
            return new CrawlWebsiteResponse(
                    "https://companya.com", "companya.com", 1, List.of(),
                    Set.of("info@companya.com"), Set.of("+919876543210"), Map.of(), Set.of(), LocalDateTime.now()
            );
        });

        asyncService.runTaskAsync(21L);

        assertEquals(TaskStatus.CANCELLED, task.getStatus());
        assertEquals("CANCELLED", task.getCurrentStage());
        assertNotNull(task.getEndTime());
        // Company A was processed and persisted, Company B was never crawled due to cancellation
        verify(pipelineService, times(1)).persistDiscoveredLead(eq(task), any(), any());
        verify(webScraperService, never()).crawlWebsite(eq("https://companyb.com"), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("Should mark task as COMPLETED_WITH_NO_RESULTS when discovery finds 0 candidates")
    void testRunTaskAsyncCompletedWithNoResults() {
        ScrapingTask task = new ScrapingTask();
        task.setId(30L);
        task.setLocation("Salem");
        task.setKeyword("UnicornSpaceShuttleRepair");
        task.setMaxResults(5);
        task.setMaxPagesPerSite(3);
        task.setStatus(TaskStatus.QUEUED);

        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));

        when(leadDiscoveryService.discoverLeads(any(LeadDiscoveryRequest.class)))
                .thenReturn(new LeadDiscoveryResponse("Salem", "UnicornSpaceShuttleRepair", 0, Collections.emptyList(), List.of("No businesses found")));

        asyncService.runTaskAsync(30L);

        assertEquals(TaskStatus.COMPLETED_WITH_NO_RESULTS, task.getStatus());
        assertEquals("COMPLETED_WITH_NO_RESULTS", task.getCurrentStage());
        assertEquals(0, task.getDiscoveredBusinesses());
        assertEquals(0, task.getLeadsSaved());
        assertEquals(100, task.getProgressPercentage());
        assertNotNull(task.getErrorMessage());
        assertTrue(task.getErrorMessage().contains("No candidate businesses found"));
        verify(webScraperService, never()).crawlWebsite(anyString(), anyInt(), anyInt(), any());
    }
}

