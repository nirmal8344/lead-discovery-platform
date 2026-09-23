package com.leaddiscovery.service;

import com.leaddiscovery.dto.CreateTaskRequest;
import com.leaddiscovery.dto.TaskProgressDto;
import com.leaddiscovery.dto.TaskResponse;
import com.leaddiscovery.dto.TaskStartResponse;
import com.leaddiscovery.entity.ScrapingTask;
import com.leaddiscovery.entity.User;
import com.leaddiscovery.entity.enums.Role;
import com.leaddiscovery.entity.enums.TaskStatus;
import com.leaddiscovery.exception.ResourceNotFoundException;
import com.leaddiscovery.repository.ScrapingTaskRepository;
import com.leaddiscovery.security.SecurityUtils;
import com.leaddiscovery.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private ScrapingTaskRepository taskRepository;

    @Mock
    private AsyncTaskExecutionService asyncExecutionService;

    @Mock
    private SecurityUtils securityUtils;

    private TaskService taskService;
    private User testUser;
    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, asyncExecutionService, securityUtils);

        testUser = new User("John Doe", "john@example.com", "hashed_pwd", Role.USER);
        testUser.setId(100L);
        testUserPrincipal = new UserPrincipal(100L, "John Doe", "john@example.com", "hashed_pwd", Role.USER, Collections.emptyList());
    }

    @Test
    @DisplayName("Should create scraping task with CREATED status and authenticated owner")
    void testCreateTask() {
        when(securityUtils.getRequiredCurrentUser()).thenReturn(testUser);
        when(taskRepository.save(any(ScrapingTask.class))).thenAnswer(inv -> {
            ScrapingTask t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        CreateTaskRequest req = new CreateTaskRequest();
        req.setLocation("Salem");
        req.setKeyword("Software");
        req.setMaxResults(10);
        req.setMaxPagesPerSite(5);

        TaskResponse res = taskService.createTask(req);
        assertNotNull(res);
        assertEquals(1L, res.getId());
        assertEquals(TaskStatus.CREATED, res.getStatus());
        verify(securityUtils, times(1)).getRequiredCurrentUser();
    }

    @Test
    @DisplayName("Should start task and transition status to QUEUED when owned by user")
    void testStartTaskSuccess() {
        ScrapingTask task = new ScrapingTask();
        task.setId(10L);
        task.setStatus(TaskStatus.CREATED);
        task.setCreatedBy(testUser);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);
        when(taskRepository.save(any(ScrapingTask.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskStartResponse response = taskService.startTask(10L);

        assertNotNull(response);
        assertEquals(10L, response.getTaskId());
        assertEquals(TaskStatus.QUEUED, response.getStatus());
        verify(asyncExecutionService, times(1)).runTaskAsync(10L);
    }

    @Test
    @DisplayName("Should deny starting a task owned by another user (IDOR prevention)")
    void testStartTaskOwnedByOtherUser() {
        User otherUser = new User("Other User", "other@example.com", "hashed_pwd", Role.USER);
        otherUser.setId(200L);

        ScrapingTask task = new ScrapingTask();
        task.setId(10L);
        task.setStatus(TaskStatus.CREATED);
        task.setCreatedBy(otherUser);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);

        assertThrows(AccessDeniedException.class, () -> taskService.startTask(10L));
        verify(asyncExecutionService, never()).runTaskAsync(anyLong());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when starting an already running task")
    void testStartTaskAlreadyRunning() {
        ScrapingTask task = new ScrapingTask();
        task.setId(10L);
        task.setStatus(TaskStatus.RUNNING);
        task.setCreatedBy(testUser);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);

        assertThrows(IllegalStateException.class, () -> taskService.startTask(10L));
    }

    @Test
    @DisplayName("Should cancel running task and signal background service when owned by user")
    void testCancelTaskSuccess() {
        ScrapingTask task = new ScrapingTask();
        task.setId(10L);
        task.setStatus(TaskStatus.RUNNING);
        task.setCreatedBy(testUser);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);
        when(taskRepository.save(any(ScrapingTask.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.cancelTask(10L);

        assertNotNull(res);
        assertEquals(TaskStatus.CANCELLED, res.getStatus());
        verify(asyncExecutionService, times(1)).requestCancellation(10L);
    }

    @Test
    @DisplayName("Should return task progress DTO for task owner")
    void testGetTaskProgress() {
        ScrapingTask task = new ScrapingTask();
        task.setId(10L);
        task.setStatus(TaskStatus.RUNNING);
        task.setProgressPercentage(45);
        task.setDiscoveredBusinesses(5);
        task.setLeadsSaved(2);
        task.setCreatedBy(testUser);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(testUserPrincipal);

        TaskProgressDto progress = taskService.getTaskProgress(10L);

        assertNotNull(progress);
        assertEquals(10L, progress.getTaskId());
        assertEquals(45, progress.getProgressPercentage());
        assertEquals(5, progress.getTotalDiscovered());
        assertEquals(2, progress.getTotalSaved());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent task ID")
    void testTaskNotFound() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTask(999L));
        assertThrows(ResourceNotFoundException.class, () -> taskService.startTask(999L));
        assertThrows(ResourceNotFoundException.class, () -> taskService.cancelTask(999L));
    }
}
