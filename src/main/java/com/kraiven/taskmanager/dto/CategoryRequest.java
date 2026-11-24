package com.kraiven.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Validated input fields for creating or updating a category. */
public record CategoryRequest(
        @NotBlank(message = "Category name is mandatory")
        @Size(max = 255, message = "Category name can be up to 255 characters")
        String name,

        @Size(max = 255, message = "Category description can be up to 255 characters")
        String description) {
}
