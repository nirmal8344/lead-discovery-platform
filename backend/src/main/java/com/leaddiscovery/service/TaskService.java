package com.leaddiscovery.service;

import com.leaddiscovery.dto.CreateTaskRequest;
import com.leaddiscovery.dto.TaskProgressDto;
import com.leaddiscovery.dto.TaskResponse;
import com.leaddiscovery.dto.TaskStartResponse;
import com.leaddiscovery.entity.ScrapingTask;
import com.leaddiscovery.entity.User;
import com.leaddiscovery.entity.enums.TaskStatus;
import com.leaddiscovery.exception.ResourceNotFoundException;
import com.leaddiscovery.repository.ScrapingTaskRepository;
import com.leaddiscovery.security.SecurityUtils;
import com.leaddiscovery.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final ScrapingTaskRepository taskRepository;
    private final AsyncTaskExecutionService asyncExecutionService;
    private final SecurityUtils securityUtils;

    public TaskService(ScrapingTaskRepository taskRepository,
                       AsyncTaskExecutionService asyncExecutionService,
                       SecurityUtils securityUtils) {
        this.taskRepository = taskRepository;
        this.asyncExecutionService = asyncExecutionService;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        User currentUser = securityUtils.getRequiredCurrentUser();

        ScrapingTask task = new ScrapingTask();
        task.setLocation(request.getLocation().trim());
        task.setKeyword(request.getKeyword().trim());
        task.setMaxResults(request.getMaxResults());
        task.setMaxPagesPerSite(request.getMaxPagesPerSite());
        task.setSearchRadiusKm(request.getSearchRadiusKm());
        task.setMaxCrawlDepth(request.getMaxCrawlDepth() != null ? request.getMaxCrawlDepth() : 3);
        task.setRequiredFields(request.getRequiredFields());
        task.setOptionalFilters(request.getOptionalFilters());
        task.setStatus(TaskStatus.CREATED);
        task.setCreatedBy(currentUser);

        ScrapingTask saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public TaskStartResponse startTask(Long taskId) {
        ScrapingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Scraping task not found with id: " + taskId));

        validateTaskOwnership(task);

        if (task.getStatus() == TaskStatus.RUNNING || task.getStatus() == TaskStatus.QUEUED || asyncExecutionService.isTaskRunning(taskId)) {
            throw new IllegalStateException("Task is already running or queued (ID: " + taskId + ")");
        }
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.COMPLETED_WITH_NO_RESULTS || task.getStatus() == TaskStatus.PARTIALLY_COMPLETED) {
            throw new IllegalStateException("Task has already finished with status: " + task.getStatus());
        }
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Cannot start a task that has been cancelled");
        }

        task.setStatus(TaskStatus.QUEUED);
        task.setCurrentStage("QUEUED");
        taskRepository.save(task);

        // Trigger background worker asynchronously
        asyncExecutionService.runTaskAsync(taskId);

        return new TaskStartResponse(taskId, TaskStatus.QUEUED, "Task accepted for background processing");
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> listTasks(Pageable pageable) {
        if (securityUtils.isAdmin()) {
            return taskRepository.findAllByOrderByCreatedAtDesc(pageable)
                    .map(TaskResponse::fromEntity);
        }

        UserPrincipal principal = securityUtils.getRequiredCurrentUserPrincipal();
        return taskRepository.findByCreatedByIdOrderByCreatedAtDesc(principal.getId(), pageable)
                .map(TaskResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId) {
        ScrapingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Scraping task not found with id: " + taskId));

        validateTaskOwnership(task);
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse cancelTask(Long taskId) {
        ScrapingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Scraping task not found with id: " + taskId));

        validateTaskOwnership(task);

        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.COMPLETED_WITH_NO_RESULTS || task.getStatus() == TaskStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel a task that is already " + task.getStatus());
        }

        asyncExecutionService.requestCancellation(taskId);
        task.setStatus(TaskStatus.CANCELLED);
        task.setCurrentStage("CANCELLED");
        ScrapingTask saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public TaskProgressDto getTaskProgress(Long taskId) {
        ScrapingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Scraping task not found with id: " + taskId));

        validateTaskOwnership(task);
        return TaskProgressDto.fromEntity(task);
    }

    public void validateTaskOwnership(ScrapingTask task) {
        if (securityUtils.isAdmin()) {
            return;
        }
        UserPrincipal principal = securityUtils.getRequiredCurrentUserPrincipal();
        if (task.getCreatedBy() == null || !task.getCreatedBy().getId().equals(principal.getId())) {
            throw new AccessDeniedException("You do not have permission to access or modify this task");
        }
    }
}
