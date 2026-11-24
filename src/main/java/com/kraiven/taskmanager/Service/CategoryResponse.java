package com.kraiven.taskmanager.Service;

import com.kraiven.taskmanager.entity.Category;

/** API response shape for a category. */
public record CategoryResponse(
        Long categoryId,
        String name,
        String description
) {

    /** Converts a database category to the fields safe for API clients. */
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription());
    }
}
