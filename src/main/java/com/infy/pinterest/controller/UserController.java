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
@RequestMapping("/users")
@Tag(name = "User Management", description = "User management APIs")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(
            @PathVariable String userId) {
        log.info("GET /auth/user/{} - Fetching user details", userId);

        UserResponseDTO response = userService.getUserById(userId);
        return ResponseEntity
                .ok(ApiResponse.success("User retrieved successfully", response));
    }


    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user profile with statistics")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getUserProfile(
            @PathVariable String userId, @RequestHeader(value = "X-User-Id", required = false) String viewerId) {
        log.info("GET /users/profile/{} - Getting user profile", userId);
        UserProfileDTO profile = userService.getUserProfile(userId, viewerId);
        return ResponseEntity
                .ok(ApiResponse.success("Profile retrieved successfully", profile));
    }
    @GetMapping("/stats/{userId}")
    @Operation(summary = "Get user profile statistics")
    public ResponseEntity<ApiResponse<ProfileStatsDTO>> getProfileStats(
            @PathVariable String userId) {
        log.info("GET /users/stats/{} - Getting profile statistics", userId);
        ProfileStatsDTO stats = userService.getProfileStats(userId);
        return ResponseEntity
                .ok(ApiResponse.success("Statistics retrieved successfully", stats));
    }
    @PutMapping("/{userId}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateProfile(
            @RequestHeader("X-User-Id") String requesterId,
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateDTO updateDTO) {

        log.info("PUT /users/{} - Updating profile", userId);

        // Ensure user can only update their own profile
        if (!requesterId.equals(userId)) {
            throw new UnauthorizedAccessException("You can only update your own profile");
        }

        UserResponseDTO response = userService.updateProfile(userId, updateDTO);
        return ResponseEntity
                .ok(ApiResponse.success("Profile updated successfully", response));
    }
    @PostMapping(value = "/{userId}/profile-picture", consumes =
            MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile picture")
    public ResponseEntity<ApiResponse<UserResponseDTO>> uploadProfilePicture(
            @RequestHeader("X-User-Id") String requesterId,
            @PathVariable String userId,
            @RequestParam("file") MultipartFile file) {

        log.info("POST /users/{}/profile-picture - Uploading profile picture", userId);

        if (!requesterId.equals(userId)) {
            throw new UnauthorizedAccessException("You can only update your own profile picture");}
                    UserResponseDTO response = userService.uploadProfilePicture(userId, file);
            return ResponseEntity
                    .ok(ApiResponse.success("Profile picture uploaded successfully", response));
        }
        @DeleteMapping("/{userId}/profile-picture")
        @Operation(summary = "Delete profile picture")
        public ResponseEntity<ApiResponse<UserResponseDTO>> deleteProfilePicture(
                @RequestHeader("X-User-Id") String requesterId,
                @PathVariable String userId) {

            log.info("DELETE /users/{}/profile-picture - Deleting profile picture", userId);

            if (!requesterId.equals(userId)) {
                throw new UnauthorizedAccessException("You can only delete your own profile picture");
            }

            UserResponseDTO response = userService.deleteProfilePicture(userId);
            return ResponseEntity
                    .ok(ApiResponse.success("Profile picture deleted successfully", response));
        }
        @PostMapping("/{userId}/deactivate")
        @Operation(summary = "Deactivate account")
        public ResponseEntity<ApiResponse<Object>> deactivateAccount(
                @RequestHeader("X-User-Id") String requesterId,
                @PathVariable String userId) {

            log.info("POST /users/{}/deactivate - Deactivating account", userId);

            if (!requesterId.equals(userId)) {
                throw new UnauthorizedAccessException("You can only deactivate your own account");
            }

            userService.deactivateAccount(userId);
            return ResponseEntity
                    .ok(ApiResponse.success("Account deactivated successfully", null));
        }
        @PostMapping("/{userId}/reactivate")
        @Operation(summary = "Reactivate account")
        public ResponseEntity<ApiResponse<Object>> reactivateAccount(
                @PathVariable String userId) {

            log.info("POST /users/{}/reactivate - Reactivating account", userId);
            userService.reactivateAccount(userId);
            return ResponseEntity
                    .ok(ApiResponse.success("Account reactivated successfully", null));

        }
        @GetMapping("/search")
        @Operation(summary = "Search users")
        public ResponseEntity<ApiResponse<PaginatedResponse<UserResponseDTO>>> searchUsers(
                @RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

            log.info("GET /users/search - Searching users with keyword: {}", keyword);
            PaginatedResponse<UserResponseDTO> response = userService.searchUsers(keyword, page,
                    size);
            return ResponseEntity
                    .ok(ApiResponse.success("Users retrieved successfully", response));
        }

    }
