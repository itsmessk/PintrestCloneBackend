package com.infy.pinterest.controller;




import com.infy.pinterest.dto.*;
import com.infy.pinterest.exception.UnauthorizedAccessException;
import com.infy.pinterest.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication APIs")
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<UserResponseDTO>> registerUser(
            @Valid @RequestBody UserRegistrationDTO registrationDTO) {
        log.info("POST /auth/register - Registering new user");

        UserResponseDTO response = userService.registerUser(registrationDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> loginUser(
            @Valid @RequestBody UserLoginDTO loginDTO) {
        log.info("POST /auth/login - User login attempt");

        LoginResponseDTO response = userService.loginUser(loginDTO);
        return ResponseEntity
                .ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "Request password reset")
    public ResponseEntity<ApiResponse<Object>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDTO requestDTO) {
        log.info("POST /auth/password-reset/request - Password reset requested");

        userService.requestPasswordReset(requestDTO);
        return ResponseEntity
                .ok(ApiResponse.success("OTP sent to registered mobile number", null));
    }

    @PostMapping("/password-reset/verify")
    @Operation(summary = "Verify OTP and reset password")
    public ResponseEntity<ApiResponse<Object>> verifyAndResetPassword(
            @Valid @RequestBody PasswordResetVerifyDTO verifyDTO) { log.info("POST /auth/password-reset/verify - Verifying OTP and resetting password");

        userService.verifyAndResetPassword(verifyDTO);
        return ResponseEntity
                .ok(ApiResponse.success("Password reset successfully", null));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<ApiResponse<Object>> logoutUser() {
        log.info("POST /auth/logout - User logout");

        // In JWT-based auth, logout is handled client-side by removing token
        // Optionally, you can maintain a token blacklist in Redis/Database
        return ResponseEntity
                .ok(ApiResponse.success("Logged out successfully", null));
    }



}
