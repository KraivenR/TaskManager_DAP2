package com.kraiven.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body containing the username and password used to log in. */
@Data 
@AllArgsConstructor 
@NoArgsConstructor 
public class AuthenticationRequest {

    private String username;
    private String password;

}
