package com.kraiven.taskmanager.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public registration input with basic username and password validation. */
@Data
@NoArgsConstructor
public class userDTO {
    
    @NotBlank(message = "username is mandatory")
    @Size(min = 5, max = 20, message = "username must be between 5 and 20 characters")
    private String username;

    @NotBlank(message = "password is mandatory")
    @Size(min = 8, max=16, message = "Password can be between 8 to 16 characters")
    private String password;
}
