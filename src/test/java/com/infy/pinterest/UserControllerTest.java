package com.infy.pinterest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.pinterest.controller.UserController;
import com.infy.pinterest.dto.*;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    private static final String USER_ID = "user-123";
    private static final String OTHER_USER_ID = "user-456";
    private static final String USER_ID_HEADER = "X-User-Id";

    private UserResponseDTO userResponseDTO;
    private UserProfileDTO userProfileDTO;
    private ProfileStatsDTO profileStatsDTO;
    private UserUpdateDTO userUpdateDTO;
    private PaginatedResponse<UserResponseDTO> paginatedResponse;

    @BeforeEach
    void setUp() {
        // Setup user response DTO
        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setUserId(USER_ID);
        userResponseDTO.setUsername("testuser");
        userResponseDTO.setEmail("test@example.com");
        userResponseDTO.setFullName("Test User");
        userResponseDTO.setBio("Test bio");
        userResponseDTO.setProfilePictureUrl("https://example.com/profile.jpg");
        userResponseDTO.setAccountType("PERSONAL");
        userResponseDTO.setCreatedAt(LocalDateTime.now());

        // Setup profile stats
        profileStatsDTO = new ProfileStatsDTO();
        profileStatsDTO.setTotalPins(50L);
        profileStatsDTO.setTotalBoards(10L);
        profileStatsDTO.setFollowers(100L);
        profileStatsDTO.setFollowing(75L);
        profileStatsDTO.setTotalPinSaves(500L);
        profileStatsDTO.setTotalPinLikes(1000L);

        // Setup user profile DTO
        userProfileDTO = new UserProfileDTO();
        userProfileDTO.setUserId(USER_ID);
        userProfileDTO.setUsername("testuser");
        userProfileDTO.setEmail("test@example.com");
        userProfileDTO.setFullName("Test User");
        userProfileDTO.setBio("Test bio");
        userProfileDTO.setProfilePictureUrl("https://example.com/profile.jpg");
        userProfileDTO.setMobileNumber("1234567890");
        userProfileDTO.setAccountType("PERSONAL");
        userProfileDTO.setIsActive(true);
        userProfileDTO.setCreatedAt(LocalDateTime.now());
        userProfileDTO.setStats(profileStatsDTO);
        userProfileDTO.setIsFollowing(false);
        userProfileDTO.setIsFollower(false);
        userProfileDTO.setIsBlocked(false);

        // Setup user update DTO
        userUpdateDTO = new UserUpdateDTO();
        userUpdateDTO.setFullName("Updated Name");
        userUpdateDTO.setBio("Updated bio");
        userUpdateDTO.setMobileNumber("9876543210");
        userUpdateDTO.setAccountType("BUSINESS");

        // Setup paginated response
        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 20, false, false);
        paginatedResponse = new PaginatedResponse<>(
                Collections.singletonList(userResponseDTO),
                pagination
        );
    }

    // ==================== GET USER TESTS ====================

    @Test
    @DisplayName("GET /users/user/{userId} - Success")
    void testGetUserById_Success() throws Exception {
        // Arrange
        when(userService.getUserById(USER_ID))
                .thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/users/user/{userId}", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

        verify(userService, times(1)).getUserById(USER_ID);
    }

    @Test
    @DisplayName("GET /users/user/{userId} - Failure - User Not Found")
    void testGetUserById_Failure_NotFound() throws Exception {
        // Arrange
        when(userService.getUserById(USER_ID))
                .thenThrow(new ResourceNotFoundException("User not found with ID: " + USER_ID));

        // Act & Assert
        mockMvc.perform(get("/users/user/{userId}", USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserById(USER_ID);
    }

    // ==================== GET USER PROFILE TESTS ====================

    @Test
    @DisplayName("GET /users/profile/{userId} - Success - Own Profile")
    void testGetUserProfile_Success_OwnProfile() throws Exception {
        // Arrange
        when(userService.getUserProfile(USER_ID, USER_ID))
                .thenReturn(userProfileDTO);

        // Act & Assert
        mockMvc.perform(get("/users/profile/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Profile retrieved successfully"))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.stats.totalPins").value(50))
                .andExpect(jsonPath("$.data.stats.followers").value(100));

        verify(userService, times(1)).getUserProfile(USER_ID, USER_ID);
    }

    @Test
    @DisplayName("GET /users/profile/{userId} - Success - Other User Profile")
    void testGetUserProfile_Success_OtherUserProfile() throws Exception {
        // Arrange
        userProfileDTO.setIsFollowing(true);
        userProfileDTO.setIsFollower(false);
        when(userService.getUserProfile(OTHER_USER_ID, USER_ID))
                .thenReturn(userProfileDTO);

        // Act & Assert
        mockMvc.perform(get("/users/profile/{userId}", OTHER_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFollowing").value(true))
                .andExpect(jsonPath("$.data.isFollower").value(false));

        verify(userService, times(1)).getUserProfile(OTHER_USER_ID, USER_ID);
    }

    @Test
    @DisplayName("GET /users/profile/{userId} - Success - Without Viewer ID (Anonymous)")
    void testGetUserProfile_Success_Anonymous() throws Exception {
        // Arrange
        when(userService.getUserProfile(USER_ID, null))
                .thenReturn(userProfileDTO);

        // Act & Assert
        mockMvc.perform(get("/users/profile/{userId}", USER_ID))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userService, times(1)).getUserProfile(USER_ID, null);
    }

    // ==================== GET PROFILE STATS TESTS ====================

    @Test
    @DisplayName("GET /users/stats/{userId} - Success")
    void testGetProfileStats_Success() throws Exception {
        // Arrange
        when(userService.getProfileStats(USER_ID))
                .thenReturn(profileStatsDTO);

        // Act & Assert
        mockMvc.perform(get("/users/stats/{userId}", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.totalPins").value(50))
                .andExpect(jsonPath("$.data.totalBoards").value(10))
                .andExpect(jsonPath("$.data.followers").value(100))
                .andExpect(jsonPath("$.data.following").value(75))
                .andExpect(jsonPath("$.data.totalPinSaves").value(500))
                .andExpect(jsonPath("$.data.totalPinLikes").value(1000));

        verify(userService, times(1)).getProfileStats(USER_ID);
    }

    @Test
    @DisplayName("GET /users/stats/{userId} - Success - User with No Activity")
    void testGetProfileStats_Success_NoActivity() throws Exception {
        // Arrange
        ProfileStatsDTO emptyStats = new ProfileStatsDTO();
        emptyStats.setTotalPins(0L);
        emptyStats.setTotalBoards(0L);
        emptyStats.setFollowers(0L);
        emptyStats.setFollowing(0L);
        emptyStats.setTotalPinSaves(0L);
        emptyStats.setTotalPinLikes(0L);

        when(userService.getProfileStats(USER_ID))
                .thenReturn(emptyStats);

        // Act & Assert
        mockMvc.perform(get("/users/stats/{userId}", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPins").value(0));

        verify(userService, times(1)).getProfileStats(USER_ID);
    }

    // ==================== UPDATE PROFILE TESTS ====================

    @Test
    @DisplayName("PUT /users/{userId} - Success")
    void testUpdateProfile_Success() throws Exception {
        // Arrange
        userResponseDTO.setFullName("Updated Name");
        userResponseDTO.setBio("Updated bio");
        when(userService.updateProfile(eq(USER_ID), any(UserUpdateDTO.class)))
                .thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/users/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userUpdateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"));

        verify(userService, times(1)).updateProfile(eq(USER_ID), any(UserUpdateDTO.class));
    }

    @Test
    @DisplayName("PUT /users/{userId} - Success - Partial Update")
    void testUpdateProfile_Success_PartialUpdate() throws Exception {
        // Arrange
        UserUpdateDTO partialUpdate = new UserUpdateDTO();
        partialUpdate.setBio("Only bio updated");
        when(userService.updateProfile(eq(USER_ID), any(UserUpdateDTO.class)))
                .thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/users/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialUpdate)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userService, times(1)).updateProfile(eq(USER_ID), any(UserUpdateDTO.class));
    }

    @Test
    @DisplayName("PUT /users/{userId} - Failure - Unauthorized (Different User)")
    void testUpdateProfile_Failure_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/users/{userId}", OTHER_USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userUpdateDTO)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).updateProfile(anyString(), any());
    }

    @Test
    @DisplayName("PUT /users/{userId} - Validation - Bio Too Long")
    void testUpdateProfile_Validation_BioTooLong() throws Exception {
        // Arrange
        userUpdateDTO.setBio("a".repeat(501));

        // Act & Assert
        mockMvc.perform(put("/users/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateProfile(anyString(), any());
    }

    @Test
    @DisplayName("PUT /users/{userId} - Validation - Invalid Mobile Number")
    void testUpdateProfile_Validation_InvalidMobileNumber() throws Exception {
        // Arrange
        userUpdateDTO.setMobileNumber("123"); // Invalid: less than 10 digits

        // Act & Assert
        mockMvc.perform(put("/users/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateProfile(anyString(), any());
    }

    @Test
    @DisplayName("PUT /users/{userId} - Validation - Invalid Account Type")
    void testUpdateProfile_Validation_InvalidAccountType() throws Exception {
        // Arrange
        userUpdateDTO.setAccountType("INVALID");

        // Act & Assert
        mockMvc.perform(put("/users/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateProfile(anyString(), any());
    }

    // ==================== PROFILE PICTURE TESTS ====================

    @Test
    @DisplayName("POST /users/{userId}/profile-picture - Success")
    void testUploadProfilePicture_Success() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "profile.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image content".getBytes()
        );

        userResponseDTO.setProfilePictureUrl("https://example.com/new-profile.jpg");
        when(userService.uploadProfilePicture(eq(USER_ID), any()))
                .thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(multipart("/users/{userId}/profile-picture", USER_ID)
                        .file(imageFile)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Profile picture uploaded successfully"))
                .andExpect(jsonPath("$.data.profilePictureUrl").value("https://example.com/new-profile.jpg"));

        verify(userService, times(1)).uploadProfilePicture(eq(USER_ID), any());
    }

    @Test
    @DisplayName("POST /users/{userId}/profile-picture - Failure - Unauthorized")
    void testUploadProfilePicture_Failure_Unauthorized() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "profile.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/users/{userId}/profile-picture", OTHER_USER_ID)
                        .file(imageFile)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).uploadProfilePicture(anyString(), any());
    }

    @Test
    @DisplayName("DELETE /users/{userId}/profile-picture - Success")
    void testDeleteProfilePicture_Success() throws Exception {
        // Arrange
        userResponseDTO.setProfilePictureUrl(null);
        when(userService.deleteProfilePicture(USER_ID))
                .thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(delete("/users/{userId}/profile-picture", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Profile picture deleted successfully"))
                .andExpect(jsonPath("$.data.profilePictureUrl").doesNotExist());

        verify(userService, times(1)).deleteProfilePicture(USER_ID);
    }

    @Test
    @DisplayName("DELETE /users/{userId}/profile-picture - Failure - Unauthorized")
    void testDeleteProfilePicture_Failure_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/users/{userId}/profile-picture", OTHER_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteProfilePicture(anyString());
    }

    // ==================== ACCOUNT DEACTIVATION TESTS ====================

    @Test
    @DisplayName("POST /users/{userId}/deactivate - Success")
    void testDeactivateAccount_Success() throws Exception {
        // Arrange
        doNothing().when(userService).deactivateAccount(USER_ID);

        // Act & Assert
        mockMvc.perform(post("/users/{userId}/deactivate", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Account deactivated successfully"));

        verify(userService, times(1)).deactivateAccount(USER_ID);
    }

    @Test
    @DisplayName("POST /users/{userId}/deactivate - Failure - Unauthorized")
    void testDeactivateAccount_Failure_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/users/{userId}/deactivate", OTHER_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).deactivateAccount(anyString());
    }

    @Test
    @DisplayName("POST /users/{userId}/reactivate - Success")
    void testReactivateAccount_Success() throws Exception {
        // Arrange
        doNothing().when(userService).reactivateAccount(USER_ID);

        // Act & Assert
        mockMvc.perform(post("/users/{userId}/reactivate", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Account reactivated successfully"));

        verify(userService, times(1)).reactivateAccount(USER_ID);
    }

    // ==================== SEARCH USERS TESTS ====================

    @Test
    @DisplayName("GET /users/search - Success")
    void testSearchUsers_Success() throws Exception {
        // Arrange
        when(userService.searchUsers("test", 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/users/search")
                        .param("keyword", "test"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].username").value("testuser"));

        verify(userService, times(1)).searchUsers("test", 0, 20);
    }

    @Test
    @DisplayName("GET /users/search - Success - Custom Pagination")
    void testSearchUsers_Success_CustomPagination() throws Exception {
        // Arrange
        when(userService.searchUsers("test", 1, 10))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/users/search")
                        .param("keyword", "test")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userService, times(1)).searchUsers("test", 1, 10);
    }

    @Test
    @DisplayName("GET /users/search - Success - Empty Results")
    void testSearchUsers_Success_EmptyResults() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<UserResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(userService.searchUsers("nonexistent", 0, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/users/search")
                        .param("keyword", "nonexistent"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        verify(userService, times(1)).searchUsers("nonexistent", 0, 20);
    }

    @Test
    @DisplayName("GET /users/search - Success - Multiple Results")
    void testSearchUsers_Success_MultipleResults() throws Exception {
        // Arrange
        UserResponseDTO user2 = new UserResponseDTO();
        user2.setUserId("user-789");
        user2.setUsername("testuser2");

        PaginationDTO pagination = new PaginationDTO(0, 1, 2L, 20, false, false);
        PaginatedResponse<UserResponseDTO> response = new PaginatedResponse<>(
                Arrays.asList(userResponseDTO, user2),
                pagination
        );

        when(userService.searchUsers("test", 0, 20))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/users/search")
                        .param("keyword", "test"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.pagination.totalElements").value(2));

        verify(userService, times(1)).searchUsers("test", 0, 20);
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    @DisplayName("Complete Profile Update Workflow - Update, Get Profile, Get Stats")
    void testCompleteProfileUpdateWorkflow() throws Exception {
        // Arrange
        when(userService.updateProfile(eq(USER_ID), any(UserUpdateDTO.class)))
                .thenReturn(userResponseDTO);
        when(userService.getUserProfile(USER_ID, USER_ID))
                .thenReturn(userProfileDTO);
        when(userService.getProfileStats(USER_ID))
                .thenReturn(profileStatsDTO);

        // Update profile
        mockMvc.perform(put("/users/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userUpdateDTO)))
                .andExpect(status().isOk());

        // Get profile
        mockMvc.perform(get("/users/profile/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Get stats
        mockMvc.perform(get("/users/stats/{userId}", USER_ID))
                .andExpect(status().isOk());

        verify(userService, times(1)).updateProfile(eq(USER_ID), any(UserUpdateDTO.class));
        verify(userService, times(1)).getUserProfile(USER_ID, USER_ID);
        verify(userService, times(1)).getProfileStats(USER_ID);
    }

    @Test
    @DisplayName("Profile Picture Workflow - Upload, Delete")
    void testProfilePictureWorkflow() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "profile.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image content".getBytes()
        );

        when(userService.uploadProfilePicture(eq(USER_ID), any()))
                .thenReturn(userResponseDTO);
        when(userService.deleteProfilePicture(USER_ID))
                .thenReturn(userResponseDTO);

        // Upload
        mockMvc.perform(multipart("/users/{userId}/profile-picture", USER_ID)
                        .file(imageFile)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Delete
        mockMvc.perform(delete("/users/{userId}/profile-picture", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        verify(userService, times(1)).uploadProfilePicture(eq(USER_ID), any());
        verify(userService, times(1)).deleteProfilePicture(USER_ID);
    }

    @Test
    @DisplayName("Account Lifecycle - Deactivate, Reactivate")
    void testAccountLifecycle() throws Exception {
        // Arrange
        doNothing().when(userService).deactivateAccount(USER_ID);
        doNothing().when(userService).reactivateAccount(USER_ID);

        // Deactivate
        mockMvc.perform(post("/users/{userId}/deactivate", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Reactivate
        mockMvc.perform(post("/users/{userId}/reactivate", USER_ID))
                .andExpect(status().isOk());

        verify(userService, times(1)).deactivateAccount(USER_ID);
        verify(userService, times(1)).reactivateAccount(USER_ID);
    }

    @Test
    @DisplayName("Edge Case - Update Profile with Only Full Name")
    void testUpdateProfile_OnlyFullName() throws Exception {
        // Arrange
        UserUpdateDTO onlyNameUpdate = new UserUpdateDTO();
        onlyNameUpdate.setFullName("New Name Only");
        when(userService.updateProfile(eq(USER_ID), any(UserUpdateDTO.class)))
                .thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/users/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onlyNameUpdate)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userService, times(1)).updateProfile(eq(USER_ID), any(UserUpdateDTO.class));
    }

    @Test
    @DisplayName("Edge Case - Change Account Type from PERSONAL to BUSINESS")
    void testUpdateProfile_ChangeAccountType() throws Exception {
        // Arrange
        UserUpdateDTO accountTypeUpdate = new UserUpdateDTO();
        accountTypeUpdate.setAccountType("BUSINESS");
        userResponseDTO.setAccountType("BUSINESS");
        when(userService.updateProfile(eq(USER_ID), any(UserUpdateDTO.class)))
                .thenReturn(userResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/users/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountTypeUpdate)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountType").value("BUSINESS"));

        verify(userService, times(1)).updateProfile(eq(USER_ID), any(UserUpdateDTO.class));
    }
}
