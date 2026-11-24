package com.kraiven.taskmanager.dto;

import java.time.LocalDate;

import com.kraiven.taskmanager.entity.Task.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Input fields accepted when creating or updating a task. */
public record TaskRequest(
    @NotBlank(message = "Title is mandatory")
    @Size(min = 3, max=100, message = "Title must be between 3 and 100 characters")
     String title,

    @Size(max=500, message = "Description can be up to 500 characters")
    String description,
    String priority,
    LocalDate deadline,
    TaskStatus status,
    Long categoryId

) {

}
