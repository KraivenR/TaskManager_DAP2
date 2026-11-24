package com.kraiven.taskmanager.Service;

import java.time.Instant;
import java.time.LocalDate;

import com.kraiven.taskmanager.entity.Task;
import com.kraiven.taskmanager.entity.TaskAuditEntry;

/** API response shape for one task-history event. */
public record TaskAuditResponse(
        Long taskId,
        String actorUsername,
        TaskAuditEntry.Action action,
        Instant occurredAt,
        String title,
        String description,
        String priority,
        LocalDate deadline,
        Task.TaskStatus status,
        String categoryName) {

    /** Maps the stored audit snapshot to its public response fields. */
    public static TaskAuditResponse from(TaskAuditEntry entry) {
        return new TaskAuditResponse(
                entry.getTaskId(),
                entry.getActorUsername(),
                entry.getAction(),
                entry.getOccurredAt(),
                entry.getTitle(),
                entry.getDescription(),
                entry.getPriority(),
                entry.getDeadline(),
                entry.getStatus(),
                entry.getCategoryName());
    }
}
