package com.kraiven.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Login response containing the JWT required by protected endpoints. */
@AllArgsConstructor
@Getter

public class AuthenticationResponse {
    final private String jwt;


}
