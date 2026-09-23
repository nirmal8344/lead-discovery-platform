package com.leaddiscovery.service;

import com.leaddiscovery.dto.CrawlWebsiteResponse;
import com.leaddiscovery.dto.DiscoveredBusinessDto;
import com.leaddiscovery.dto.LeadDiscoveryRequest;
import com.leaddiscovery.dto.LeadDiscoveryResponse;
import com.leaddiscovery.dto.OrganizationIdentity;
import com.leaddiscovery.entity.ScrapingLog;
import com.leaddiscovery.entity.ScrapingTask;
import com.leaddiscovery.entity.enums.IdentityValidationResult;
import com.leaddiscovery.entity.enums.TaskStatus;
import com.leaddiscovery.repository.ScrapingLogRepository;
import com.leaddiscovery.repository.ScrapingTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AsyncTaskExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskExecutionService.class);

    private final ScrapingTaskRepository taskRepository;
    private final ScrapingLogRepository logRepository;
    private final LeadDiscoveryService leadDiscoveryService;
    private final WebScraperService webScraperService;
    private final LeadDiscoveryPipelineService pipelineService;

    private final Set<Long> runningTasks = ConcurrentHashMap.newKeySet();
    private final Set<Long> cancelledTasks = ConcurrentHashMap.newKeySet();

    public AsyncTaskExecutionService(ScrapingTaskRepository taskRepository,
                                     ScrapingLogRepository logRepository,
                                     LeadDiscoveryService leadDiscoveryService,
                                     WebScraperService webScraperService,
                                     LeadDiscoveryPipelineService pipelineService) {
        this.taskRepository = taskRepository;
        this.logRepository = logRepository;
        this.leadDiscoveryService = leadDiscoveryService;
        this.webScraperService = webScraperService;
        this.pipelineService = pipelineService;
    }

    public boolean isTaskRunning(Long taskId) {
        return runningTasks.contains(taskId);
    }

    public void requestCancellation(Long taskId) {
        cancelledTasks.add(taskId);
    }

    @Async("scrapingTaskExecutor")
    public void runTaskAsync(Long taskId) {
        if (!runningTasks.add(taskId)) {
            log.warn("[TASK_START] Task {} is already actively running in another worker thread.", taskId);
            return;
        }

        log.info("[TASK_START] Started background worker for ScrapingTask ID: {}", taskId);

        try {
            Optional<ScrapingTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                log.error("[TASK_ERROR] Background runner could not find task {}", taskId);
                return;
            }

            ScrapingTask task = taskOpt.get();
            if (cancelledTasks.contains(taskId) || task.getStatus() == TaskStatus.CANCELLED) {
                log.info("[TASK_STAGE] Task {} was cancelled prior to worker startup.", taskId);
                task.setStatus(TaskStatus.CANCELLED);
                task.setCurrentStage("CANCELLED");
                task.setEndTime(LocalDateTime.now());
                taskRepository.saveAndFlush(task);
                return;
            }

            // 1. Stage: DISCOVERING (10%)
            task.setStatus(TaskStatus.RUNNING);
            task.setStartTime(LocalDateTime.now());
            task.setCurrentStage("DISCOVERING");
            task.setProgressPercentage(10);
            task = taskRepository.saveAndFlush(task);

            log.info("[TASK_STAGE] Task {}: stage=DISCOVERING, progress=10%", taskId);
            logTaskEvent(task, "INFO", "Started background discovery for '" + task.getKeyword() + " in " + task.getLocation() + "'");

            // 2. Discover business candidates
            LeadDiscoveryRequest discoveryRequest = new LeadDiscoveryRequest(
                    task.getLocation(), task.getKeyword(), task.getMaxResults(), task.getSearchRadiusKm()
            );
            LeadDiscoveryResponse discoveryResponse = leadDiscoveryService.discoverLeads(discoveryRequest);

            if (checkCancellation(task)) {
                return;
            }

            List<DiscoveredBusinessDto> candidates = discoveryResponse.getBusinesses();
            int totalDiscovered = candidates != null ? candidates.size() : 0;
            task.setDiscoveredBusinesses(totalDiscovered);

            // 3. Stage: VALIDATING_CANDIDATES (25%)
            task.setCurrentStage("VALIDATING_CANDIDATES");
            task.setProgressPercentage(25);
            task = taskRepository.saveAndFlush(task);

            log.info("[TASK_STAGE] Task {}: stage=VALIDATING_CANDIDATES, progress=25%, totalDiscovered={}",
                    taskId, totalDiscovered);
            logTaskEvent(task, "INFO", "Discovered " + totalDiscovered + " candidate websites to validate");

            int totalSaved = 0;
            int totalCrawled = 0;
            int totalFailed = 0;
            int processedCount = 0;

            if (totalDiscovered > 0) {
                // 4. Crawl, extract organization identity, score, and persist each candidate
                for (DiscoveredBusinessDto candidate : candidates) {
                    if (checkCancellation(task)) {
                        return;
                    }

                    processedCount++;
                    String websiteUrl = candidate.getWebsiteUrl();
                    String rawCandidateName = candidate.getBusinessName();

                    try {
                        // Stage: CRAWLING_WEBSITES (30% - 75%)
                        task.setCurrentStage("CRAWLING_WEBSITES");
                        int crawlProgress = 30 + (int) (((double) (processedCount - 1) / totalDiscovered) * 45);
                        task.setProgressPercentage(Math.min(75, Math.max(30, crawlProgress)));
                        task = taskRepository.saveAndFlush(task);

                        log.info("[TASK_PROGRESS] Task {}: [{}/{}] crawling {} ({})",
                                taskId, processedCount, totalDiscovered, rawCandidateName, websiteUrl);

                        int maxCrawlDepth = task.getMaxCrawlDepth() != null ? task.getMaxCrawlDepth() : 3;
                        CrawlWebsiteResponse crawlResponse = webScraperService.crawlWebsite(
                                websiteUrl, task.getMaxPagesPerSite(), maxCrawlDepth, task.getRequiredFields()
                        );
                        totalCrawled++;

                        // Verify candidate identity status
                        OrganizationIdentity identity = crawlResponse.getOrganizationIdentity();
                        if (identity != null && identity.getIdentityStatus() == IdentityValidationResult.IDENTITY_REJECTED) {
                            log.warn("[TASK_PROGRESS] Task {}: Skipping rejected candidate identity '{}' ({}): {}",
                                    taskId, rawCandidateName, websiteUrl, identity.getRejectionReason());
                            logTaskEvent(task, "WARN", "Rejected candidate " + rawCandidateName + ": " + identity.getRejectionReason());
                            totalFailed++;
                            task.setFailedRecords(totalFailed);
                            task.setProcessedWebsites(totalCrawled);
                            taskRepository.saveAndFlush(task);
                            continue;
                        }

                        // Stage: SAVING_LEADS (75% - 90%)
                        task.setCurrentStage("SAVING_LEADS");
                        int saveProgress = 75 + (int) (((double) processedCount / totalDiscovered) * 15);
                        task.setProgressPercentage(Math.min(90, saveProgress));
                        task = taskRepository.saveAndFlush(task);

                        pipelineService.persistDiscoveredLead(task, candidate, crawlResponse);
                        totalSaved++;

                        task.setLeadsSaved(totalSaved);
                        task.setProcessedWebsites(totalCrawled);
                        task = taskRepository.saveAndFlush(task);

                        log.info("[TASK_PROGRESS] Task {}: [{}/{}] successfully saved verified lead '{}'",
                                taskId, processedCount, totalDiscovered, rawCandidateName);

                    } catch (Exception e) {
                        totalFailed++;
                        task.setFailedRecords(totalFailed);
                        task.setProcessedWebsites(totalCrawled);
                        log.warn("[TASK_ERROR] Task {}: error processing candidate '{}' ({}): {}",
                                taskId, rawCandidateName, websiteUrl, e.getMessage());
                        logTaskEvent(task, "WARN", "Failed to process " + rawCandidateName + " (" + websiteUrl + "): " + e.getMessage());
                        task = taskRepository.saveAndFlush(task);
                    }
                }
            }

            if (checkCancellation(task)) {
                return;
            }

            // 5. Final Stage: SCORING (95%) -> Terminal status (100%)
            task.setCurrentStage("SCORING");
            task.setProgressPercentage(95);
            task = taskRepository.saveAndFlush(task);
            log.info("[TASK_STAGE] Task {}: stage=SCORING, progress=95%, totalSaved={}, totalFailed={}",
                    taskId, totalSaved, totalFailed);

            if (totalSaved > 0) {
                task.setStatus(TaskStatus.COMPLETED);
                task.setCurrentStage("COMPLETED");
                task.setProgressPercentage(100);
                task.setEndTime(LocalDateTime.now());
                task = taskRepository.saveAndFlush(task);
                log.info("[TASK_COMPLETE] Task {}: status=COMPLETED, saved={}, crawled={}, failed={}",
                        taskId, totalSaved, totalCrawled, totalFailed);
                logTaskEvent(task, "INFO", "Background execution completed: " + totalSaved + " verified leads saved, " + totalFailed + " failed/rejected");
            } else if (totalDiscovered == 0) {
                task.setStatus(TaskStatus.COMPLETED_WITH_NO_RESULTS);
                task.setCurrentStage("COMPLETED_WITH_NO_RESULTS");
                task.setProgressPercentage(100);
                task.setErrorMessage("No candidate businesses found matching keyword '" + task.getKeyword() + "' and location '" + task.getLocation() + "'.");
                task.setEndTime(LocalDateTime.now());
                task = taskRepository.saveAndFlush(task);
                log.info("[TASK_COMPLETE] Task {}: status=COMPLETED_WITH_NO_RESULTS", taskId);
                logTaskEvent(task, "WARN", "Completed with no candidate businesses found for '" + task.getKeyword() + " in " + task.getLocation() + "'");
            } else {
                task.setStatus(TaskStatus.FAILED);
                task.setCurrentStage("FAILED");
                task.setProgressPercentage(100);
                task.setErrorMessage("Discovered " + totalDiscovered + " candidates, but failed to extract verified organization identities.");
                task.setEndTime(LocalDateTime.now());
                task = taskRepository.saveAndFlush(task);
                log.info("[TASK_COMPLETE] Task {}: status=FAILED, discovered={}", taskId, totalDiscovered);
                logTaskEvent(task, "ERROR", "Failed to verify organization identity for all " + totalDiscovered + " discovered candidates.");
            }

        } catch (Exception e) {
            log.error("[TASK_ERROR] Fatal error executing background task {}: {}", taskId, e.getMessage(), e);
            try {
                Optional<ScrapingTask> taskOpt = taskRepository.findById(taskId);
                if (taskOpt.isPresent()) {
                    ScrapingTask task = taskOpt.get();
                    task.setStatus(TaskStatus.FAILED);
                    task.setErrorMessage(e.getMessage());
                    task.setEndTime(LocalDateTime.now());
                    task.setCurrentStage("FAILED");
                    taskRepository.saveAndFlush(task);
                    logTaskEvent(task, "ERROR", "Task failed: " + e.getMessage());
                }
            } catch (Exception ignored) {
            }
        } finally {
            runningTasks.remove(taskId);
            cancelledTasks.remove(taskId);
        }
    }

    private boolean checkCancellation(ScrapingTask task) {
        if (cancelledTasks.contains(task.getId())) {
            log.info("[TASK_STAGE] Task {} received cancellation signal during background execution.", task.getId());
            task.setStatus(TaskStatus.CANCELLED);
            task.setCurrentStage("CANCELLED");
            task.setEndTime(LocalDateTime.now());
            taskRepository.saveAndFlush(task);
            logTaskEvent(task, "INFO", "Task was cancelled by user.");
            return true;
        }
        return false;
    }

    private void logTaskEvent(ScrapingTask task, String level, String message) {
        try {
            ScrapingLog entry = new ScrapingLog();
            entry.setScrapingTask(task);
            entry.setLevel(level);
            entry.setMessage(message);
            entry.setContext("AsyncTaskExecutionService");
            logRepository.save(entry);
        } catch (Exception ignored) {
        }
    }
}
