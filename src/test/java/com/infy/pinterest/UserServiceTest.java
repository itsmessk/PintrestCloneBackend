package com.infy.pinterest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import com.infy.pinterest.dto.LoginResponseDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PasswordResetRequestDTO;
import com.infy.pinterest.dto.PasswordResetVerifyDTO;
import com.infy.pinterest.dto.ProfileStatsDTO;
import com.infy.pinterest.dto.UserLoginDTO;
import com.infy.pinterest.dto.UserProfileDTO;
import com.infy.pinterest.dto.UserRegistrationDTO;
import com.infy.pinterest.dto.UserResponseDTO;
import com.infy.pinterest.dto.UserUpdateDTO;
import com.infy.pinterest.entity.Pin;
import com.infy.pinterest.entity.User;
import com.infy.pinterest.exception.AccountLockedException;
import com.infy.pinterest.exception.InvalidCredentialsException;
import com.infy.pinterest.exception.PasswordMismatchException;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.exception.UserAlreadyExistsException;
import com.infy.pinterest.repository.BlockedUserRepository;
import com.infy.pinterest.repository.BoardRepository;
import com.infy.pinterest.repository.FollowRepository;
import com.infy.pinterest.repository.PinRepository;
import com.infy.pinterest.repository.UserRepository;
import com.infy.pinterest.service.UserService;
import com.infy.pinterest.utility.FileUploadService;
import com.infy.pinterest.utility.JwtUtil;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PinRepository pinRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserRegistrationDTO registrationDTO;
    private UserLoginDTO loginDTO;
    private UserUpdateDTO updateDTO;
    private PasswordResetRequestDTO resetRequestDTO;
    private PasswordResetVerifyDTO resetVerifyDTO;
    private BCryptPasswordEncoder passwordEncoder;
    private MultipartFile mockImage;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        // Setup test user
        testUser = new User();
        testUser.setUserId("user-123");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        testUser.setFullName("Test User");
        testUser.setBio("Test bio");
        testUser.setProfilePictureUrl("https://example.com/profile.jpg");
        testUser.setMobileNumber("9876543210");
        testUser.setAccountType(User.AccountType.personal);
        testUser.setIsActive(true);
        testUser.setFailedLoginAttempts(0);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // Setup registration DTO
        registrationDTO = new UserRegistrationDTO();
        registrationDTO.setUsername("newuser");
        registrationDTO.setEmail("newuser@example.com");
        registrationDTO.setPassword("Password@123");
        registrationDTO.setConfirmPassword("Password@123");

        // Setup login DTO
        loginDTO = new UserLoginDTO();
        loginDTO.setEmail("test@example.com");
        loginDTO.setPassword("Password@123");

        // Setup update DTO
        updateDTO = new UserUpdateDTO();
        updateDTO.setFullName("Updated Name");
        updateDTO.setBio("Updated bio");
        updateDTO.setMobileNumber("9999999999");
        updateDTO.setAccountType("BUSINESS");

        // Setup password reset DTOs
        resetRequestDTO = new PasswordResetRequestDTO();
        resetRequestDTO.setEmail("test@example.com");

        resetVerifyDTO = new PasswordResetVerifyDTO();
        resetVerifyDTO.setEmail("test@example.com");
        resetVerifyDTO.setOtp("123456");
        resetVerifyDTO.setNewPassword("NewPass@123");
        resetVerifyDTO.setConfirmPassword("NewPass@123");

        // Mock image
        mockImage = mock(MultipartFile.class);
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    void testRegisterUser_Success() {
        // Arrange
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.registerUser(registrationDTO);

        // Assert
        assertNotNull(result);
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterUser_PasswordMismatch() {
        // Arrange
        registrationDTO.setConfirmPassword("DifferentPassword@123");

        // Act & Assert
        assertThrows(PasswordMismatchException.class, () -> {
            userService.registerUser(registrationDTO);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(registrationDTO);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_UsernameAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(registrationDTO);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void testLoginUser_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token-123");

        // Act
        LoginResponseDTO result = userService.loginUser(loginDTO);

        // Assert
        assertNotNull(result);
        assertEquals("user-123", result.getUserId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("jwt-token-123", result.getToken());
        assertEquals(3600, result.getExpiresIn());
        verify(userRepository).findByEmail("test@example.com");
        verify(jwtUtil).generateToken("user-123", "testuser", "test@example.com");
    }

    @Test
    void testLoginUser_InvalidEmail() {
        // Arrange
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.empty());
        loginDTO.setEmail("wrong@example.com");

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.loginUser(loginDTO);
        });

        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString());
    }

    @Test
    void testLoginUser_InvalidPassword() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        loginDTO.setPassword("WrongPassword@123");

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.loginUser(loginDTO);
        });

        verify(userRepository).save(any(User.class)); // Failed attempt should be saved
        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString());
    }

    @Test
    void testLoginUser_AccountLocked() {
        // Arrange
        testUser.setFailedLoginAttempts(3);
        testUser.setLastFailedLoginAt(LocalDateTime.now().minusSeconds(30));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(AccountLockedException.class, () -> {
            userService.loginUser(loginDTO);
        });

        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString());
    }

    @Test
    void testLoginUser_ResetFailedAttemptsAfterLockExpiry() {
        // Arrange
        testUser.setFailedLoginAttempts(3);
        testUser.setLastFailedLoginAt(LocalDateTime.now().minusSeconds(65)); // Lock expired
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token-123");

        // Act
        LoginResponseDTO result = userService.loginUser(loginDTO);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(2)).save(any(User.class)); // Reset + successful login reset
    }

    @Test
    void testLoginUser_IncrementFailedAttempts() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        loginDTO.setPassword("WrongPassword@123");

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.loginUser(loginDTO);
        });

        verify(userRepository).save(any(User.class));
    }

    @Test
    void testLoginUser_ResetFailedAttemptsOnSuccess() {
        // Arrange
        testUser.setFailedLoginAttempts(2);
        testUser.setLastFailedLoginAt(LocalDateTime.now().minusSeconds(30));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token-123");

        // Act
        LoginResponseDTO result = userService.loginUser(loginDTO);

        // Assert
        assertNotNull(result);
        verify(userRepository).save(any(User.class)); // Should reset failed attempts
    }

    // ==================== GET USER PROFILE TESTS ====================

    @Test
    void testGetUserProfile_Success_OwnProfile() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(User.class), eq(UserProfileDTO.class))).thenReturn(new UserProfileDTO());
        when(pinRepository.countByUserId("user-123")).thenReturn(10L);
        when(boardRepository.countByUserId("user-123")).thenReturn(5L);
        when(followRepository.countByFollowingId("user-123")).thenReturn(100L);
        when(followRepository.countByFollowerId("user-123")).thenReturn(50L);
        when(pinRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        UserProfileDTO result = userService.getUserProfile("user-123", "user-123");

        // Assert
        assertNotNull(result);
        verify(userRepository).findById("user-123");
        verify(followRepository, never()).existsByFollowerIdAndFollowingId(anyString(), anyString());
    }

    @Test
    void testGetUserProfile_Success_ViewingOtherProfile() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(User.class), eq(UserProfileDTO.class))).thenReturn(new UserProfileDTO());
        when(pinRepository.countByUserId("user-123")).thenReturn(10L);
        when(boardRepository.countByUserId("user-123")).thenReturn(5L);
        when(followRepository.countByFollowingId("user-123")).thenReturn(100L);
        when(followRepository.countByFollowerId("user-123")).thenReturn(50L);
        when(pinRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());
        when(followRepository.existsByFollowerIdAndFollowingId("viewer-456", "user-123")).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFollowingId("user-123", "viewer-456")).thenReturn(false);
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("viewer-456", "user-123")).thenReturn(false);

        // Act
        UserProfileDTO result = userService.getUserProfile("user-123", "viewer-456");

        // Assert
        assertNotNull(result);
        verify(followRepository).existsByFollowerIdAndFollowingId("viewer-456", "user-123");
        verify(followRepository).existsByFollowerIdAndFollowingId("user-123", "viewer-456");
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("viewer-456", "user-123");
    }

    @Test
    void testGetUserProfile_UserNotFound() {
        // Arrange
        when(userRepository.findById("user-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserProfile("user-999", "viewer-123");
        });
    }

    @Test
    void testGetUserProfile_WithNullViewer() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(User.class), eq(UserProfileDTO.class))).thenReturn(new UserProfileDTO());
        when(pinRepository.countByUserId("user-123")).thenReturn(10L);
        when(boardRepository.countByUserId("user-123")).thenReturn(5L);
        when(followRepository.countByFollowingId("user-123")).thenReturn(100L);
        when(followRepository.countByFollowerId("user-123")).thenReturn(50L);
        when(pinRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        UserProfileDTO result = userService.getUserProfile("user-123", null);

        // Assert
        assertNotNull(result);
        verify(followRepository, never()).existsByFollowerIdAndFollowingId(anyString(), anyString());
    }

    // ==================== GET PROFILE STATS TESTS ====================

    @Test
    void testGetProfileStats_Success() {
        // Arrange
        Pin pin1 = new Pin();
        pin1.setSaveCount(10);
        pin1.setLikeCount(20);

        Pin pin2 = new Pin();
        pin2.setSaveCount(5);
        pin2.setLikeCount(15);

        when(pinRepository.countByUserId("user-123")).thenReturn(2L);
        when(boardRepository.countByUserId("user-123")).thenReturn(3L);
        when(followRepository.countByFollowingId("user-123")).thenReturn(100L);
        when(followRepository.countByFollowerId("user-123")).thenReturn(50L);
        when(pinRepository.findByUserId("user-123")).thenReturn(Arrays.asList(pin1, pin2));

        // Act
        ProfileStatsDTO result = userService.getProfileStats("user-123");

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getTotalPins());
        assertEquals(3L, result.getTotalBoards());
        assertEquals(100L, result.getFollowers());
        assertEquals(50L, result.getFollowing());
        assertEquals(15L, result.getTotalPinSaves());
        assertEquals(35L, result.getTotalPinLikes());
    }

    @Test
    void testGetProfileStats_EmptyStats() {
        // Arrange
        when(pinRepository.countByUserId("user-123")).thenReturn(0L);
        when(boardRepository.countByUserId("user-123")).thenReturn(0L);
        when(followRepository.countByFollowingId("user-123")).thenReturn(0L);
        when(followRepository.countByFollowerId("user-123")).thenReturn(0L);
        when(pinRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        ProfileStatsDTO result = userService.getProfileStats("user-123");

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.getTotalPins());
        assertEquals(0L, result.getTotalBoards());
        assertEquals(0L, result.getFollowers());
        assertEquals(0L, result.getFollowing());
        assertEquals(0L, result.getTotalPinSaves());
        assertEquals(0L, result.getTotalPinLikes());
    }

    // ==================== UPDATE PROFILE TESTS ====================

    @Test
    void testUpdateProfile_Success_AllFields() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.updateProfile("user-123", updateDTO);

        // Assert
        assertNotNull(result);
        verify(userRepository).findById("user-123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateProfile_PartialUpdate_OnlyFullName() {
        // Arrange
        UserUpdateDTO partialUpdate = new UserUpdateDTO();
        partialUpdate.setFullName("New Name Only");

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.updateProfile("user-123", partialUpdate);

        // Assert
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateProfile_PartialUpdate_OnlyBio() {
        // Arrange
        UserUpdateDTO partialUpdate = new UserUpdateDTO();
        partialUpdate.setBio("New bio only");

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.updateProfile("user-123", partialUpdate);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testUpdateProfile_UserNotFound() {
        // Arrange
        when(userRepository.findById("user-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateProfile("user-999", updateDTO);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateProfile_ChangeAccountType() {
        // Arrange
        UserUpdateDTO accountTypeUpdate = new UserUpdateDTO();
        accountTypeUpdate.setAccountType("BUSINESS");

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.updateProfile("user-123", accountTypeUpdate);

        // Assert
        assertNotNull(result);
    }

    // ==================== PROFILE PICTURE TESTS ====================

    @Test
    void testUploadProfilePicture_Success() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(fileUploadService.uploadImage(any(MultipartFile.class))).thenReturn("https://example.com/new-profile.jpg");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.uploadProfilePicture("user-123", mockImage);

        // Assert
        assertNotNull(result);
        verify(fileUploadService).uploadImage(mockImage);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUploadProfilePicture_UserNotFound() {
        // Arrange
        when(userRepository.findById("user-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.uploadProfilePicture("user-999", mockImage);
        });

        verify(fileUploadService, never()).uploadImage(any(MultipartFile.class));
    }

    @Test
    void testDeleteProfilePicture_Success() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.deleteProfilePicture("user-123");

        // Assert
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testDeleteProfilePicture_UserNotFound() {
        // Arrange
        when(userRepository.findById("user-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteProfilePicture("user-999");
        });

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== ACCOUNT ACTIVATION TESTS ====================

    @Test
    void testDeactivateAccount_Success() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.deactivateAccount("user-123");

        // Assert
        verify(userRepository).findById("user-123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testDeactivateAccount_UserNotFound() {
        // Arrange
        when(userRepository.findById("user-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deactivateAccount("user-999");
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testReactivateAccount_Success() {
        // Arrange
        testUser.setIsActive(false);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.reactivateAccount("user-123");

        // Assert
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testReactivateAccount_UserNotFound() {
        // Arrange
        when(userRepository.findById("user-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.reactivateAccount("user-999");
        });
    }

    // ==================== SEARCH USERS TESTS ====================

    @Test
    void testSearchUsers_Success() {
        // Arrange
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 10), 1);

        when(userRepository.searchUsers(eq("test"), any(Pageable.class))).thenReturn(userPage);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        PaginatedResponse<UserResponseDTO> result = userService.searchUsers("test", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(0, result.getPagination().getCurrentPage());
        verify(userRepository).searchUsers(eq("test"), any(Pageable.class));
    }

    @Test
    void testSearchUsers_NoResults() {
        // Arrange
        Page<User> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);

        when(userRepository.searchUsers(eq("nonexistent"), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<UserResponseDTO> result = userService.searchUsers("nonexistent", 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getPagination().getTotalPages());
    }

    @Test
    void testSearchUsers_Pagination() {
        // Arrange
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(2, 5), 30);

        when(userRepository.searchUsers(eq("test"), any(Pageable.class))).thenReturn(userPage);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        PaginatedResponse<UserResponseDTO> result = userService.searchUsers("test", 2, 5);

        // Assert
        assertEquals(2, result.getPagination().getCurrentPage());
        assertEquals(6, result.getPagination().getTotalPages());
        assertTrue(result.getPagination().getHasNext());
        assertTrue(result.getPagination().getHasPrevious());
    }

    @Test
    void testSearchUsers_MultipleResults() {
        // Arrange
        User user1 = new User();
        user1.setUserId("user-1");
        user1.setUsername("testuser1");

        User user2 = new User();
        user2.setUserId("user-2");
        user2.setUsername("testuser2");

        List<User> users = Arrays.asList(user1, user2);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 10), 2);

        when(userRepository.searchUsers(eq("test"), any(Pageable.class))).thenReturn(userPage);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        PaginatedResponse<UserResponseDTO> result = userService.searchUsers("test", 0, 10);

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals(2L, result.getPagination().getTotalItems());
    }

    // ==================== PASSWORD RESET TESTS ====================

    @Test
    void testRequestPasswordReset_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        userService.requestPasswordReset(resetRequestDTO);

        // Assert
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void testRequestPasswordReset_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        resetRequestDTO.setEmail("nonexistent@example.com");

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.requestPasswordReset(resetRequestDTO);
        });
    }

    @Test
    void testVerifyAndResetPassword_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.verifyAndResetPassword(resetVerifyDTO);

        // Assert
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testVerifyAndResetPassword_PasswordMismatch() {
        // Arrange
        resetVerifyDTO.setConfirmPassword("DifferentPassword@123");

        // Act & Assert
        assertThrows(PasswordMismatchException.class, () -> {
            userService.verifyAndResetPassword(resetVerifyDTO);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testVerifyAndResetPassword_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        resetVerifyDTO.setEmail("nonexistent@example.com");

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.verifyAndResetPassword(resetVerifyDTO);
        });
    }

    // ==================== GET USER BY ID TESTS ====================

    @Test
    void testGetUserById_Success() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.getUserById("user-123");

        // Assert
        assertNotNull(result);
        verify(userRepository).findById("user-123");
    }

    @Test
    void testGetUserById_UserNotFound() {
        // Arrange
        when(userRepository.findById("user-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById("user-999");
        });
    }

    // ==================== EDGE CASES AND INTEGRATION TESTS ====================

    @Test
    void testRegisterUser_WithPersonalAccountType() {
        // Arrange
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.registerUser(registrationDTO);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testUpdateProfile_WithNullValues() {
        // Arrange
        UserUpdateDTO nullUpdate = new UserUpdateDTO();
        // All fields are null

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.updateProfile("user-123", nullUpdate);

        // Assert
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testGetProfileStats_WithNoPins() {
        // Arrange
        when(pinRepository.countByUserId("user-123")).thenReturn(0L);
        when(boardRepository.countByUserId("user-123")).thenReturn(5L);
        when(followRepository.countByFollowingId("user-123")).thenReturn(10L);
        when(followRepository.countByFollowerId("user-123")).thenReturn(20L);
        when(pinRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        ProfileStatsDTO result = userService.getProfileStats("user-123");

        // Assert
        assertEquals(0L, result.getTotalPins());
        assertEquals(5L, result.getTotalBoards());
    }

    @Test
    void testLoginUser_MultipleFailedAttempts() {
        // Arrange
        testUser.setFailedLoginAttempts(2);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        loginDTO.setPassword("WrongPassword@123");

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.loginUser(loginDTO);
        });

        verify(userRepository).save(any(User.class));
    }

    @Test
    void testGetUserProfile_WithBlocked() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(User.class), eq(UserProfileDTO.class))).thenReturn(new UserProfileDTO());
        when(pinRepository.countByUserId("user-123")).thenReturn(10L);
        when(boardRepository.countByUserId("user-123")).thenReturn(5L);
        when(followRepository.countByFollowingId("user-123")).thenReturn(100L);
        when(followRepository.countByFollowerId("user-123")).thenReturn(50L);
        when(pinRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());
        when(followRepository.existsByFollowerIdAndFollowingId("viewer-456", "user-123")).thenReturn(false);
        when(followRepository.existsByFollowerIdAndFollowingId("user-123", "viewer-456")).thenReturn(false);
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("viewer-456", "user-123")).thenReturn(true);

        // Act
        UserProfileDTO result = userService.getUserProfile("user-123", "viewer-456");

        // Assert
        assertNotNull(result);
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("viewer-456", "user-123");
    }

    @Test
    void testUploadProfilePicture_ReplaceExisting() {
        // Arrange
        testUser.setProfilePictureUrl("https://example.com/old-profile.jpg");
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(fileUploadService.uploadImage(any(MultipartFile.class))).thenReturn("https://example.com/new-profile.jpg");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.uploadProfilePicture("user-123", mockImage);

        // Assert
        assertNotNull(result);
        verify(fileUploadService).uploadImage(mockImage);
    }

    @Test
    void testDeleteProfilePicture_WhenNoExistingPicture() {
        // Arrange
        testUser.setProfilePictureUrl(null);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = userService.deleteProfilePicture("user-123");

        // Assert
        assertNotNull(result);
    }
}
