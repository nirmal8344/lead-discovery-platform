package com.leaddiscovery.dto;

import com.leaddiscovery.entity.enums.TaskStatus;

public class TaskStartResponse {

    private Long taskId;
    private TaskStatus status;
    private String message;

    public TaskStartResponse() {
    }

    public TaskStartResponse(Long taskId, TaskStatus status, String message) {
        this.taskId = taskId;
        this.status = status;
        this.message = message;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
