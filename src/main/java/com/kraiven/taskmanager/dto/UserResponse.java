package com.kraiven.taskmanager.dto;

import com.kraiven.taskmanager.entity.Role;
import com.kraiven.taskmanager.entity.User;

/** Safe user response that deliberately leaves out the password. */
public record UserResponse(Long id, String username, Role role) {

    /** Copies only public account fields from the database entity. */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
