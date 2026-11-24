package com.kraiven.taskmanager.Service;

import java.time.LocalDate;

import com.kraiven.taskmanager.entity.Category;
import com.kraiven.taskmanager.entity.Task;
import com.kraiven.taskmanager.entity.Task.TaskStatus;

import lombok.Builder;

/** Public task response fields, with category data mapped to a small DTO. */
@Builder 
public record taskResponse(

    Long id,
    String title,
    String description,
    String priority,
    LocalDate deadline,
    TaskStatus status,
    CategoryResponse category

) {

    /** Converts a task entity without exposing its user relationship. */
    public static taskResponse from(Task task) {
        Category taskCategory = task.getCategory();
        CategoryResponse categoryResponse = taskCategory == null
                ? null
                : new CategoryResponse(
                        taskCategory.getId(),
                        taskCategory.getName(),
                        taskCategory.getDescription());

        return new taskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getDeadline(),
                task.getStatus(),
                categoryResponse);
    }
}
