package com.infy.pinterest.controller;

import com.infy.pinterest.dto.ApiResponse;
import com.infy.pinterest.dto.BlockedUserDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.service.BlockedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blocked-users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Blocked Users", description = "APIs for managing blocked users")
public class BlockedUserController {

    private final BlockedUserService blockedUserService;

    @PostMapping("/block/{targetUserId}")
    @Operation(summary = "Block a user")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String targetUserId) {
        
        log.info("POST /blocked-users/block/{} - Blocking user", targetUserId);
        blockedUserService.blockUser(currentUserId, targetUserId);
        
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", null));
    }

    @DeleteMapping("/unblock/{targetUserId}")
    @Operation(summary = "Unblock a user")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String targetUserId) {
        
        log.info("DELETE /blocked-users/unblock/{} - Unblocking user", targetUserId);
        blockedUserService.unblockUser(currentUserId, targetUserId);
        
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", null));
    }

    @GetMapping("/check/{targetUserId}")
    @Operation(summary = "Check if a user is blocked")
    public ResponseEntity<ApiResponse<Boolean>> checkIfBlocked(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String targetUserId) {
        
        log.info("GET /blocked-users/check/{} - Checking block status", targetUserId);
        boolean isBlocked = blockedUserService.isBlockedInEitherDirection(currentUserId, targetUserId);
        
        return ResponseEntity.ok(ApiResponse.success("Block status retrieved", isBlocked));
    }

    @GetMapping
    @Operation(summary = "Get list of blocked users")
    public ResponseEntity<ApiResponse<PaginatedResponse<BlockedUserDTO>>> getBlockedUsers(
            @RequestHeader("X-User-Id") String currentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /blocked-users - Fetching blocked users for user {}", currentUserId);
        PaginatedResponse<BlockedUserDTO> response = blockedUserService.getBlockedUsers(currentUserId, page, size);
        
        return ResponseEntity.ok(ApiResponse.success("Blocked users retrieved successfully", response));
    }
}
