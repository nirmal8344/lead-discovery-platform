package com.leaddiscovery.controller;

import com.leaddiscovery.dto.CreateTaskRequest;
import com.leaddiscovery.dto.TaskErrorDto;
import com.leaddiscovery.dto.TaskResponse;
import com.leaddiscovery.entity.ScrapingTaskError;
import com.leaddiscovery.repository.ScrapingTaskErrorRepository;
import com.leaddiscovery.security.SecurityUtils;
import com.leaddiscovery.security.UserPrincipal;
import com.leaddiscovery.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final com.leaddiscovery.service.LeadService leadService;
    private final ScrapingTaskErrorRepository taskErrorRepository;
    private final SecurityUtils securityUtils;

    public TaskController(TaskService taskService,
                          com.leaddiscovery.service.LeadService leadService,
                          ScrapingTaskErrorRepository taskErrorRepository,
                          SecurityUtils securityUtils) {
        this.taskService = taskService;
        this.leadService = leadService;
        this.taskErrorRepository = taskErrorRepository;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(taskService.listTasks(pageable));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTask(taskId));
    }

    @PostMapping("/{taskId}/start")
    public ResponseEntity<com.leaddiscovery.dto.TaskStartResponse> startTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.startTask(taskId));
    }

    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<TaskResponse> cancelTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.cancelTask(taskId));
    }

    @GetMapping("/{taskId}/progress")
    public ResponseEntity<com.leaddiscovery.dto.TaskProgressDto> getTaskProgress(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTaskProgress(taskId));
    }

    @GetMapping("/{taskId}/leads")
    public ResponseEntity<Page<com.leaddiscovery.dto.LeadListItemDto>> getTaskLeads(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(leadService.listLeadsByTask(taskId, pageable));
    }

    /**
     * Returns the list of failed / skipped websites for a task.
     * Only the owning user (or ADMIN) can access this.
     */
    @GetMapping("/{taskId}/errors")
    public ResponseEntity<List<TaskErrorDto>> getTaskErrors(@PathVariable Long taskId) {
        // Ownership validation: ensure user owns the task
        TaskResponse task = taskService.getTask(taskId); // already secured in TaskService
        long count = taskErrorRepository.countByScrapingTaskId(taskId);

        List<ScrapingTaskError> errors = taskErrorRepository
                .findByScrapingTaskIdOrderByCreatedAtDesc(taskId);

        List<TaskErrorDto> dtos = errors.stream()
                .map(TaskErrorDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}
