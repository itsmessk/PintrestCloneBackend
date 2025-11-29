package com.infy.pinterest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDTO {

    @NotBlank(message = "Please provide a valid username")
    @Pattern(regexp = "^[a-z0-9_.-]+$",
            message = "Username must contain only lowercase letters, digits, and special characters")
    private String username;

    @NotBlank(message = "Please provide a valid email")
    @Email(message = "Email must be in valid format")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.(com|org|in)$",
            message = "Email must have valid domain (com, org, or in)")
    private String email;

    @NotBlank(message = "Please provide a valid password")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$",
            message = "Password must be 8-16 characters with uppercase, lowercase, digit, and special character")
    private String password;

    @NotBlank(message = "Please provide a valid confirm password")
    private String confirmPassword;
}
