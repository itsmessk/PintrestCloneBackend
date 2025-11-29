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
public class PasswordResetVerifyDTO {

    @NotBlank(message = "Please provide a valid email")
    @Email(message = "Email must be in valid format")
    private String email;

    @NotBlank(message = "Please provide a valid OTP")
    private String otp;

    @NotBlank(message = "Please provide a valid password")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$",
            message = "Password must be 8-16 characters with uppercase, lowercase, digit, and special character")
    private String newPassword;

    @NotBlank(message = "Please provide a valid confirm password")
    private String confirmPassword;
}
