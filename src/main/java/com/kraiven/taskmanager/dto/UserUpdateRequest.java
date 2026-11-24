package com.kraiven.taskmanager.dto;

import com.kraiven.taskmanager.entity.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** defines what an admin can change for a use */
public record UserUpdateRequest(
        @NotBlank(message = "Username is mandatory")
        @Size(min = 5, max = 20, message = "Username must be between 5 and 20 characters")
        String username,

        @NotNull(message = "Role is mandatory")
        Role role,

        @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters")
        String password) {
}
