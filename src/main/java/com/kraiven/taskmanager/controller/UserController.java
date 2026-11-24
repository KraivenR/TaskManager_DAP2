package com.kraiven.taskmanager.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kraiven.taskmanager.Service.UserService;
import com.kraiven.taskmanager.dto.UserResponse;
import com.kraiven.taskmanager.dto.UserUpdateRequest;
import com.kraiven.taskmanager.dto.userDTO;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Handles public registration and admin-only user management endpoints. */
@RestController
@Tag(name = "Users", description = "Register accounts and administer users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Registration always creates a regular USER account, never an admin account. */
    @PostMapping("/api/auth/register")
    public ResponseEntity<UserResponse> saveUser(@Valid @RequestBody userDTO userDto) {
        UserResponse created = UserResponse.from(
                userService.register(userDto.getUsername(), userDto.getPassword()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Lists users without exposing their password hashes. */
    @GetMapping("/api/v1/users")
    @SecurityRequirement(name = "bearerAuth")
    public List<UserResponse> findAll() {
        return userService.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    /** Returns one user's public response fields. */
    @GetMapping("/api/v1/users/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse findById(@PathVariable Long id) {
        return UserResponse.from(userService.findById(id));
    }

    /** Lets an administrator change a user's account details and role. */
    @PutMapping("/api/v1/users/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return UserResponse.from(userService.update(id, request));
    }

    /** Refuses deletion when the user still owns tasks. */
    @DeleteMapping("/api/v1/users/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
