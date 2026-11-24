package com.kraiven.taskmanager.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Immutable-style snapshot of a task action, kept even when the task is deleted. */
@Entity
@Table(
        name = "task_audit_entries",
        indexes = @Index(name = "idx_task_audit_task_id_occurred_at", columnList = "task_id, occurred_at"))
public class TaskAuditEntry {

    public enum Action {
        CREATED,
        UPDATED,
        DELETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "task_owner_username")
    private String taskOwnerUsername;

    @Column(name = "actor_username", nullable = false)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String priority;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private Task.TaskStatus status;

    @Column(name = "category_name")
    private String categoryName;

    protected TaskAuditEntry() {
    }

    public TaskAuditEntry(
            Long taskId,
            String taskOwnerUsername,
            String actorUsername,
            Action action,
            Instant occurredAt,
            String title,
            String description,
            String priority,
            LocalDate deadline,
            Task.TaskStatus status,
            String categoryName) {
        this.taskId = taskId;
        this.taskOwnerUsername = taskOwnerUsername;
        this.actorUsername = actorUsername;
        this.action = action;
        this.occurredAt = occurredAt;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.deadline = deadline;
        this.status = status;
        this.categoryName = categoryName;
    }

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getTaskOwnerUsername() {
        return taskOwnerUsername;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public Action getAction() {
        return action;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPriority() {
        return priority;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public Task.TaskStatus getStatus() {
        return status;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
