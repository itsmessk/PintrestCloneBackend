package com.infy.pinterest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestDTO {

    @NotBlank(message = "Please provide a valid email")
    @Email(message = "Email must be in valid format")
    private String email;
}
