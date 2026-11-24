package com.kraiven.taskmanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kraiven.taskmanager.entity.TaskAuditEntry;

/** Stores audit entries and retrieves a task's events in chronological order. */
public interface TaskAuditRepository extends JpaRepository<TaskAuditEntry, Long> {

    List<TaskAuditEntry> findByTaskIdOrderByOccurredAtAscIdAsc(Long taskId);

    List<TaskAuditEntry> findByTaskIdAndTaskOwnerUsernameOrderByOccurredAtAscIdAsc(
            Long taskId, String taskOwnerUsername);
}
