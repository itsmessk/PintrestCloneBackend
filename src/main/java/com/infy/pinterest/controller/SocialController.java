package com.infy.pinterest.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.pinterest.dto.ApiResponse;
import com.infy.pinterest.dto.FollowStatsDTO;
import com.infy.pinterest.dto.FollowerResponseDTO;
import com.infy.pinterest.dto.FollowingResponseDTO;
import com.infy.pinterest.dto.InvitationResponseActionDTO;
import com.infy.pinterest.dto.InvitationResponseDTO;
import com.infy.pinterest.dto.InvitationSendDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.UserReportDTO;
import com.infy.pinterest.service.SocialService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;



@RestController
@RequestMapping("/social")
@Tag(name = "Social", description = "Social interaction APIs")
@SecurityRequirement(name = "JWT")
@Slf4j
public class SocialController {

    private final SocialService socialService;

    @Autowired
    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @PostMapping("/follow/{userId}")
    @Operation(summary = "Follow a user")
    public ResponseEntity<ApiResponse<Object>> followUser(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String userId) {
        socialService.followUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Successfully followed user", null));
    }

    @DeleteMapping("/unfollow/{userId}")
    @Operation(summary = "Unfollow a user")
    public ResponseEntity<ApiResponse<Object>> unfollowUser(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String userId) {
        socialService.unfollowUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Successfully unfollowed user", null));
    }

    @GetMapping("/followers/{userId}")
    @Operation(summary = "Get user's followers")
    public ResponseEntity<ApiResponse<PaginatedResponse<FollowerResponseDTO>>> getFollowers(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<FollowerResponseDTO> response = socialService.getFollowers(userId, currentUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Followers retrieved successfully", response));
    }

    @GetMapping("/following/{userId}")
    @Operation(summary = "Get user's following")
    public ResponseEntity<ApiResponse<PaginatedResponse<FollowingResponseDTO>>> getFollowing(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<FollowingResponseDTO> response = socialService.getFollowing(userId, currentUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Following retrieved successfully", response));
    }

    @GetMapping("/stats/{userId}")
    @Operation(summary = "Get follow statistics")
    public ResponseEntity<ApiResponse<FollowStatsDTO>> getFollowStats(@PathVariable String userId) {
        FollowStatsDTO stats = socialService.getFollowStats(userId);
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved successfully", stats));
    }

    @PostMapping("/invitations/send")
    @Operation(summary = "Send collaboration invitation")
    public ResponseEntity<ApiResponse<InvitationResponseDTO>> sendInvitation(
            @RequestHeader("X-User-Id") String currentUserId,
            @Valid @RequestBody InvitationSendDTO invitationDTO) {
        InvitationResponseDTO response = socialService.sendInvitation(currentUserId, invitationDTO);
        return ResponseEntity.ok(ApiResponse.success("Invitation sent successfully", response));
    }

    @GetMapping("/invitations")
    @Operation(summary = "Get user's received invitations")
    public ResponseEntity<ApiResponse<PaginatedResponse<InvitationResponseDTO>>> getInvitations(
            @RequestHeader("X-User-Id") String currentUserId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<InvitationResponseDTO> response = socialService.getInvitations(currentUserId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Invitations retrieved successfully", response));
    }

    @GetMapping("/invitations/sent")
    @Operation(summary = "Get user's sent invitations")
    public ResponseEntity<ApiResponse<PaginatedResponse<InvitationResponseDTO>>> getSentInvitations(
            @RequestHeader("X-User-Id") String currentUserId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<InvitationResponseDTO> response = socialService.getSentInvitations(currentUserId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Sent invitations retrieved successfully", response));
    }

    @GetMapping("/invitations/{invitationId}")
    @Operation(summary = "Get invitation details")
    public ResponseEntity<ApiResponse<InvitationResponseDTO>> getInvitationDetails(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String invitationId) {
        InvitationResponseDTO response = socialService.getInvitationDetails(invitationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Invitation details retrieved successfully", response));
    }

    @PutMapping("/invitations/{invitationId}")
    @Operation(summary = "Respond to invitation")
    public ResponseEntity<ApiResponse<Object>> respondToInvitation(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String invitationId,
            @Valid @RequestBody InvitationResponseActionDTO actionDTO) {
        socialService.respondToInvitation(invitationId, currentUserId, actionDTO.getAction());
        return ResponseEntity.ok(ApiResponse.success("Invitation " + actionDTO.getAction() + "ed successfully", null));
    }

    @DeleteMapping("/invitations/{invitationId}")
    @Operation(summary = "Cancel invitation")
    public ResponseEntity<ApiResponse<Object>> cancelInvitation(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String invitationId) {
        socialService.cancelInvitation(invitationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Invitation cancelled successfully", null));
    }

    @PostMapping("/block/{userId}")
    @Operation(summary = "Block a user")
    public ResponseEntity<ApiResponse<Object>> blockUser(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String userId) {
        socialService.blockUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", null));
    }

    @DeleteMapping("/unblock/{userId}")
    @Operation(summary = "Unblock a user")
    public ResponseEntity<ApiResponse<Object>> unblockUser(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String userId) {
        socialService.unblockUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", null));
    }

    @PostMapping("/report/{userId}")
    @Operation(summary = "Report a user")
    public ResponseEntity<ApiResponse<Object>> reportUser(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String userId,
            @Valid @RequestBody UserReportDTO reportDTO) {
        socialService.reportUser(currentUserId, userId, reportDTO);
        return ResponseEntity.ok(ApiResponse.success("Report submitted successfully", null));
    }

    @GetMapping("/is-following/{userId}")
    @Operation(summary = "Check if following user")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String userId) {
        Boolean isFollowing = socialService.isFollowing(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Status retrieved", isFollowing));
    }
}
