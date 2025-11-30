package com.infy.pinterest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.pinterest.controller.AuthController;
import com.infy.pinterest.dto.*;
import com.infy.pinterest.exception.*;
import com.infy.pinterest.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserRegistrationDTO validRegistrationDTO;
    private UserLoginDTO validLoginDTO;
    private PasswordResetRequestDTO validResetRequestDTO;
    private PasswordResetVerifyDTO validResetVerifyDTO;
    private UserResponseDTO userResponseDTO;
    private LoginResponseDTO loginResponseDTO;

    @BeforeEach
    void setUp() {
        // Setup valid registration DTO
        validRegistrationDTO = new UserRegistrationDTO();
        validRegistrationDTO.setUsername("testuser");
        validRegistrationDTO.setEmail("test@example.com");
        validRegistrationDTO.setPassword("Password@123");
        validRegistrationDTO.setConfirmPassword("Password@123");

        // Setup valid login DTO
        validLoginDTO = new UserLoginDTO();
        validLoginDTO.setEmail("test@example.com");
        validLoginDTO.setPassword("Password@123");

        // Setup valid password reset request DTO
        validResetRequestDTO = new PasswordResetRequestDTO();
        validResetRequestDTO.setEmail("test@example.com");

        // Setup valid password reset verify DTO
        validResetVerifyDTO = new PasswordResetVerifyDTO();
        validResetVerifyDTO.setEmail("test@example.com");
        validResetVerifyDTO.setOtp("123456");
        validResetVerifyDTO.setNewPassword("NewPass@123");
        validResetVerifyDTO.setConfirmPassword("NewPass@123");

        // Setup user response DTO
        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setUserId("user-123");
        userResponseDTO.setUsername("testuser");
        userResponseDTO.setEmail("test@example.com");
        userResponseDTO.setFullName("Test User");
        userResponseDTO.setBio("Test bio");
        userResponseDTO.setProfilePictureUrl("https://example.com/profile.jpg");
        userResponseDTO.setAccountType("personal");
        userResponseDTO.setCreatedAt(LocalDateTime.now());

        // Setup login response DTO
        loginResponseDTO = new LoginResponseDTO();
        loginResponseDTO.setUserId("user-123");
        loginResponseDTO.setUsername("testuser");
        loginResponseDTO.setEmail("test@example.com");
        loginResponseDTO.setToken("jwt-token-123");
        loginResponseDTO.setExpiresIn(3600);
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    @DisplayName("POST /auth/register - Success - Valid Registration")
    void testRegisterUser_Success() throws Exception {
        // Arrange
        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.userId").value("user-123"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Failure - Invalid Email Format")
    void testRegisterUser_InvalidEmail() throws Exception {
        // Arrange
        validRegistrationDTO.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Failure - Invalid Username Pattern")
    void testRegisterUser_InvalidUsername() throws Exception {
        // Arrange
        validRegistrationDTO.setUsername("Invalid@User");

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Failure - Weak Password")
    void testRegisterUser_WeakPassword() throws Exception {
        // Arrange
        validRegistrationDTO.setPassword("weak");
        validRegistrationDTO.setConfirmPassword("weak");

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Failure - Missing Required Fields")
    void testRegisterUser_MissingFields() throws Exception {
        // Arrange
        UserRegistrationDTO incompleteDTO = new UserRegistrationDTO();

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incompleteDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Failure - Email Already Exists")
    void testRegisterUser_EmailAlreadyExists() throws Exception {
        // Arrange
        when(userService.registerUser(any(UserRegistrationDTO.class)))
                .thenThrow(new UserAlreadyExistsException("Email already exists"));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Email already exists"));

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Failure - Username Already Exists")
    void testRegisterUser_UsernameAlreadyExists() throws Exception {
        // Arrange
        when(userService.registerUser(any(UserRegistrationDTO.class)))
                .thenThrow(new UserAlreadyExistsException("Username already exists"));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Username already exists"));

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Failure - Password Mismatch")
    void testRegisterUser_PasswordMismatch() throws Exception {
        // Arrange
        when(userService.registerUser(any(UserRegistrationDTO.class)))
                .thenThrow(new PasswordMismatchException("Passwords do not match"));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Passwords do not match"));

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Failure - Invalid Email Domain")
    void testRegisterUser_InvalidEmailDomain() throws Exception {
        // Arrange
        validRegistrationDTO.setEmail("test@example.net");

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Success - Username with Special Characters")
    void testRegisterUser_UsernameWithSpecialChars() throws Exception {
        // Arrange
        validRegistrationDTO.setUsername("test_user-123");
        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"));

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("POST /auth/login - Success - Valid Credentials")
    void testLoginUser_Success() throws Exception {
        // Arrange
        when(userService.loginUser(any(UserLoginDTO.class))).thenReturn(loginResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.userId").value("user-123"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, times(1)).loginUser(any(UserLoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Failure - Invalid Credentials")
    void testLoginUser_InvalidCredentials() throws Exception {
        // Arrange
        when(userService.loginUser(any(UserLoginDTO.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(userService, times(1)).loginUser(any(UserLoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Failure - Account Locked")
    void testLoginUser_AccountLocked() throws Exception {
        // Arrange
        when(userService.loginUser(any(UserLoginDTO.class)))
                .thenThrow(new AccountLockedException(
                        "Too many failed login attempts. Please try again after 60 seconds", 60));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andDo(print())
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Too many failed login attempts. Please try again after 60 seconds"))
                .andExpect(jsonPath("$.retryAfter").value(60));

        verify(userService, times(1)).loginUser(any(UserLoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Failure - Missing Email")
    void testLoginUser_MissingEmail() throws Exception {
        // Arrange
        validLoginDTO.setEmail(null);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).loginUser(any(UserLoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Failure - Missing Password")
    void testLoginUser_MissingPassword() throws Exception {
        // Arrange
        validLoginDTO.setPassword(null);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).loginUser(any(UserLoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Failure - Empty Email")
    void testLoginUser_EmptyEmail() throws Exception {
        // Arrange
        validLoginDTO.setEmail("");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).loginUser(any(UserLoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Failure - Empty Password")
    void testLoginUser_EmptyPassword() throws Exception {
        // Arrange
        validLoginDTO.setPassword("");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).loginUser(any(UserLoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Failure - Invalid Email Format")
    void testLoginUser_InvalidEmailFormat() throws Exception {
        // Arrange
        validLoginDTO.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).loginUser(any(UserLoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Failure - Empty Request Body")
    void testLoginUser_EmptyRequestBody() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).loginUser(any(UserLoginDTO.class));
    }

    // ==================== PASSWORD RESET REQUEST TESTS ====================

    @Test
    @DisplayName("POST /auth/password-reset/request - Success - Valid Email")
    void testRequestPasswordReset_Success() throws Exception {
        // Arrange
        doNothing().when(userService).requestPasswordReset(any(PasswordResetRequestDTO.class));

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetRequestDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("OTP sent to registered mobile number"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, times(1)).requestPasswordReset(any(PasswordResetRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/request - Failure - User Not Found")
    void testRequestPasswordReset_UserNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User not found with email: test@example.com"))
                .when(userService).requestPasswordReset(any(PasswordResetRequestDTO.class));

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetRequestDTO)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("User not found with email: test@example.com"));

        verify(userService, times(1)).requestPasswordReset(any(PasswordResetRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/request - Failure - Missing Email")
    void testRequestPasswordReset_MissingEmail() throws Exception {
        // Arrange
        validResetRequestDTO.setEmail(null);

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetRequestDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).requestPasswordReset(any(PasswordResetRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/request - Failure - Invalid Email Format")
    void testRequestPasswordReset_InvalidEmail() throws Exception {
        // Arrange
        validResetRequestDTO.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetRequestDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).requestPasswordReset(any(PasswordResetRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/request - Failure - Empty Email")
    void testRequestPasswordReset_EmptyEmail() throws Exception {
        // Arrange
        validResetRequestDTO.setEmail("");

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetRequestDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).requestPasswordReset(any(PasswordResetRequestDTO.class));
    }

    // ==================== PASSWORD RESET VERIFY TESTS ====================

    @Test
    @DisplayName("POST /auth/password-reset/verify - Success - Valid OTP and Password")
    void testVerifyAndResetPassword_Success() throws Exception {
        // Arrange
        doNothing().when(userService).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetVerifyDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Password reset successfully"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, times(1)).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/verify - Failure - Password Mismatch")
    void testVerifyAndResetPassword_PasswordMismatch() throws Exception {
        // Arrange
        doThrow(new PasswordMismatchException("Passwords do not match"))
                .when(userService).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetVerifyDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Passwords do not match"));

        verify(userService, times(1)).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/verify - Failure - User Not Found")
    void testVerifyAndResetPassword_UserNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User not found with email: test@example.com"))
                .when(userService).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetVerifyDTO)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("User not found with email: test@example.com"));

        verify(userService, times(1)).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/verify - Failure - Missing Email")
    void testVerifyAndResetPassword_MissingEmail() throws Exception {
        // Arrange
        validResetVerifyDTO.setEmail(null);

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetVerifyDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/verify - Failure - Missing OTP")
    void testVerifyAndResetPassword_MissingOTP() throws Exception {
        // Arrange
        validResetVerifyDTO.setOtp(null);

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetVerifyDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/verify - Failure - Missing New Password")
    void testVerifyAndResetPassword_MissingNewPassword() throws Exception {
        // Arrange
        validResetVerifyDTO.setNewPassword(null);

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetVerifyDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/verify - Failure - Missing Confirm Password")
    void testVerifyAndResetPassword_MissingConfirmPassword() throws Exception {
        // Arrange
        validResetVerifyDTO.setConfirmPassword(null);

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetVerifyDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/verify - Failure - Weak Password")
    void testVerifyAndResetPassword_WeakPassword() throws Exception {
        // Arrange
        validResetVerifyDTO.setNewPassword("weak");
        validResetVerifyDTO.setConfirmPassword("weak");

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetVerifyDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/verify - Failure - Invalid Email Format")
    void testVerifyAndResetPassword_InvalidEmail() throws Exception {
        // Arrange
        validResetVerifyDTO.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetVerifyDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/verify - Failure - Empty OTP")
    void testVerifyAndResetPassword_EmptyOTP() throws Exception {
        // Arrange
        validResetVerifyDTO.setOtp("");

        // Act & Assert
        mockMvc.perform(post("/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetVerifyDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(userService, never()).verifyAndResetPassword(any(PasswordResetVerifyDTO.class));
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    @DisplayName("POST /auth/logout - Success")
    void testLogoutUser_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());

        // Note: logout doesn't call service in JWT-based auth
        verifyNoInteractions(userService);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("POST /auth/register - Edge Case - Username at Minimum Length")
    void testRegisterUser_MinimumUsernameLength() throws Exception {
        // Arrange
        validRegistrationDTO.setUsername("ab");
        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Edge Case - Password at Minimum Length")
    void testRegisterUser_MinimumPasswordLength() throws Exception {
        // Arrange
        validRegistrationDTO.setPassword("Pass@123");
        validRegistrationDTO.setConfirmPassword("Pass@123");
        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Edge Case - Password at Maximum Length")
    void testRegisterUser_MaximumPasswordLength() throws Exception {
        // Arrange
        validRegistrationDTO.setPassword("Password@1234567");
        validRegistrationDTO.setConfirmPassword("Password@1234567");
        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Edge Case - Email with .org Domain")
    void testRegisterUser_EmailOrgDomain() throws Exception {
        // Arrange
        validRegistrationDTO.setEmail("test@example.org");
        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Edge Case - Email with .in Domain")
    void testRegisterUser_EmailInDomain() throws Exception {
        // Arrange
        validRegistrationDTO.setEmail("test@example.in");
        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Edge Case - Multiple Rapid Login Attempts")
    void testLoginUser_RapidAttempts() throws Exception {
        // Arrange
        when(userService.loginUser(any(UserLoginDTO.class)))
                .thenReturn(loginResponseDTO)
                .thenReturn(loginResponseDTO)
                .thenReturn(loginResponseDTO);

        // Act & Assert - 3 rapid attempts
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }

        verify(userService, times(3)).loginUser(any(UserLoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/password-reset/request - Edge Case - Multiple Reset Requests")
    void testRequestPasswordReset_MultipleRequests() throws Exception {
        // Arrange
        doNothing().when(userService).requestPasswordReset(any(PasswordResetRequestDTO.class));

        // Act & Assert - 2 rapid reset requests
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validResetRequestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }

        verify(userService, times(2)).requestPasswordReset(any(PasswordResetRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Validation - Null Request Body")
    void testRegisterUser_NullRequestBody() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Validation - Malformed JSON")
    void testLoginUser_MalformedJSON() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).loginUser(any(UserLoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Security - SQL Injection Attempt in Username")
    void testRegisterUser_SQLInjectionUsername() throws Exception {
        // Arrange
        validRegistrationDTO.setUsername("admin'--");

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // Should fail validation

        verify(userService, never()).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Security - XSS Attempt in Email")
    void testLoginUser_XSSAttempt() throws Exception {
        // Arrange
        validLoginDTO.setEmail("<script>alert('xss')</script>@example.com");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // Should fail email validation

        verify(userService, never()).loginUser(any(UserLoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register - Performance - Large Valid Payload")
    void testRegisterUser_LargeValidPayload() throws Exception {
        // Arrange
        validRegistrationDTO.setUsername("a".repeat(50)); // Max valid username
        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(userResponseDTO);

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        long responseTime = result.getAsyncResult() != null ? 
            result.getAsyncResult().hashCode() : 0; // Placeholder for timing
        
        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }
}
