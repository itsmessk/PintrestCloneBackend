package com.infy.pinterest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.pinterest.controller.SocialController;
import com.infy.pinterest.dto.*;
import com.infy.pinterest.exception.*;
import com.infy.pinterest.service.SocialService;
import com.infy.pinterest.utility.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

@ExtendWith(MockitoExtension.class)
class SocialControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private SocialService socialService;

    @InjectMocks
    private SocialController socialController;

    private static final String USER_ID = "user-123";
    private static final String TARGET_USER_ID = "user-456";
    private static final String INVITATION_ID = "inv-789";
    private static final String BOARD_ID = "board-123";
    private static final String USER_ID_HEADER = "X-User-Id";

    private FollowerResponseDTO followerResponseDTO;
    private FollowingResponseDTO followingResponseDTO;
    private FollowStatsDTO followStatsDTO;
    private InvitationSendDTO invitationSendDTO;
    private InvitationResponseDTO invitationResponseDTO;
    private InvitationResponseActionDTO invitationActionDTO;
    private UserReportDTO userReportDTO;
    private PaginatedResponse<FollowerResponseDTO> followersPaginatedResponse;
    private PaginatedResponse<FollowingResponseDTO> followingPaginatedResponse;
    private PaginatedResponse<InvitationResponseDTO> invitationsPaginatedResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(socialController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        
        // Setup follower response
        followerResponseDTO = new FollowerResponseDTO();
        followerResponseDTO.setUserId(USER_ID);
        followerResponseDTO.setUsername("testuser");
        followerResponseDTO.setFullName("Test User");
        followerResponseDTO.setProfilePictureUrl("https://example.com/profile.jpg");
        followerResponseDTO.setBio("Test bio");
        followerResponseDTO.setIsFollowing(true);
        followerResponseDTO.setFollowedAt(LocalDateTime.now());

        // Setup following response
        followingResponseDTO = new FollowingResponseDTO();
        followingResponseDTO.setUserId(TARGET_USER_ID);
        followingResponseDTO.setUsername("targetuser");
        followingResponseDTO.setFullName("Target User");
        followingResponseDTO.setProfilePictureUrl("https://example.com/target.jpg");
        followingResponseDTO.setBio("Target bio");
        followingResponseDTO.setIsFollower(false);
        followingResponseDTO.setFollowedAt(LocalDateTime.now());

        // Setup follow stats
        followStatsDTO = new FollowStatsDTO(100, 50);

        // Setup invitation send DTO
        invitationSendDTO = new InvitationSendDTO();
        invitationSendDTO.setRecipientUsername("recipient");
        invitationSendDTO.setBoardId(BOARD_ID);
        invitationSendDTO.setMessage("Join my board!");
        invitationSendDTO.setPermission("EDIT");

        // Setup invitation response
        UserSummaryDTO fromUser = new UserSummaryDTO();
        fromUser.setUserId(USER_ID);
        fromUser.setUsername("testuser");
        fromUser.setProfilePictureUrl("https://example.com/profile.jpg");

        BoardSummaryDTO board = new BoardSummaryDTO();
        board.setBoardId(BOARD_ID);
        board.setBoardName("Test Board");

        invitationResponseDTO = new InvitationResponseDTO();
        invitationResponseDTO.setInvitationId(INVITATION_ID);
        invitationResponseDTO.setType("board_collaboration");
        invitationResponseDTO.setFrom(fromUser);
        invitationResponseDTO.setBoard(board);
        invitationResponseDTO.setMessage("Join my board!");
        invitationResponseDTO.setPermission("EDIT");
        invitationResponseDTO.setStatus("PENDING");
        invitationResponseDTO.setSentAt(LocalDateTime.now());

        // Setup invitation action DTO
        invitationActionDTO = new InvitationResponseActionDTO();
        invitationActionDTO.setAction("accept");

        // Setup user report DTO
        userReportDTO = new UserReportDTO();
        userReportDTO.setReason("SPAM");
        userReportDTO.setDescription("User is posting spam content");

        // Setup paginated responses
        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 20, false, false);
        followersPaginatedResponse = new PaginatedResponse<>(
                Collections.singletonList(followerResponseDTO),
                pagination
        );
        followingPaginatedResponse = new PaginatedResponse<>(
                Collections.singletonList(followingResponseDTO),
                pagination
        );
        invitationsPaginatedResponse = new PaginatedResponse<>(
                Collections.singletonList(invitationResponseDTO),
                pagination
        );
    }

    // ==================== FOLLOW/UNFOLLOW TESTS ====================

    @Test
    @DisplayName("POST /social/follow/{userId} - Success")
    void testFollowUser_Success() throws Exception {
        // Arrange
        doNothing().when(socialService).followUser(USER_ID, TARGET_USER_ID);

        // Act & Assert
        mockMvc.perform(post("/social/follow/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Successfully followed user"));

        verify(socialService, times(1)).followUser(USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("POST /social/follow/{userId} - Failure - Self Follow")
    void testFollowUser_Failure_SelfFollow() throws Exception {
        // Arrange
        doThrow(new SelfFollowException("You cannot follow yourself"))
                .when(socialService).followUser(USER_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(post("/social/follow/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(socialService, times(1)).followUser(USER_ID, USER_ID);
    }

    @Test
    @DisplayName("POST /social/follow/{userId} - Failure - Already Following")
    void testFollowUser_Failure_AlreadyFollowing() throws Exception {
        // Arrange
        doThrow(new AlreadyFollowingException("You are already following this user"))
                .when(socialService).followUser(USER_ID, TARGET_USER_ID);

        // Act & Assert
        mockMvc.perform(post("/social/follow/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isConflict());

        verify(socialService, times(1)).followUser(USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("DELETE /social/unfollow/{userId} - Success")
    void testUnfollowUser_Success() throws Exception {
        // Arrange
        doNothing().when(socialService).unfollowUser(USER_ID, TARGET_USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/social/unfollow/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Successfully unfollowed user"));

        verify(socialService, times(1)).unfollowUser(USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("DELETE /social/unfollow/{userId} - Failure - Not Following")
    void testUnfollowUser_Failure_NotFollowing() throws Exception {
        // Arrange
        doThrow(new NotFollowingException("You are not following this user"))
                .when(socialService).unfollowUser(USER_ID, TARGET_USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/social/unfollow/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(socialService, times(1)).unfollowUser(USER_ID, TARGET_USER_ID);
    }

    // ==================== GET FOLLOWERS/FOLLOWING TESTS ====================

    @Test
    @DisplayName("GET /social/followers/{userId} - Success")
    void testGetFollowers_Success() throws Exception {
        // Arrange
        when(socialService.getFollowers(TARGET_USER_ID, USER_ID, 0, 20))
                .thenReturn(followersPaginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/social/followers/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Followers retrieved successfully"))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].username").value("testuser"))
                .andExpect(jsonPath("$.data.data[0].isFollowing").value(true));

        verify(socialService, times(1)).getFollowers(TARGET_USER_ID, USER_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /social/followers/{userId} - Success - Custom Pagination")
    void testGetFollowers_Success_CustomPagination() throws Exception {
        // Arrange
        when(socialService.getFollowers(TARGET_USER_ID, USER_ID, 1, 10))
                .thenReturn(followersPaginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/social/followers/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(socialService, times(1)).getFollowers(TARGET_USER_ID, USER_ID, 1, 10);
    }

    @Test
    @DisplayName("GET /social/following/{userId} - Success")
    void testGetFollowing_Success() throws Exception {
        // Arrange
        when(socialService.getFollowing(USER_ID, USER_ID, 0, 20))
                .thenReturn(followingPaginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/social/following/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Following retrieved successfully"))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].username").value("targetuser"))
                .andExpect(jsonPath("$.data.data[0].isFollower").value(false));

        verify(socialService, times(1)).getFollowing(USER_ID, USER_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /social/following/{userId} - Success - Empty List")
    void testGetFollowing_Success_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<FollowingResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(socialService.getFollowing(USER_ID, USER_ID, 0, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/social/following/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        verify(socialService, times(1)).getFollowing(USER_ID, USER_ID, 0, 20);
    }

    // ==================== FOLLOW STATS TESTS ====================

    @Test
    @DisplayName("GET /social/stats/{userId} - Success")
    void testGetFollowStats_Success() throws Exception {
        // Arrange
        when(socialService.getFollowStats(USER_ID))
                .thenReturn(followStatsDTO);

        // Act & Assert
        mockMvc.perform(get("/social/stats/{userId}", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Stats retrieved successfully"))
                .andExpect(jsonPath("$.data.followers").value(100))
                .andExpect(jsonPath("$.data.following").value(50));

        verify(socialService, times(1)).getFollowStats(USER_ID);
    }

    // ==================== INVITATION TESTS ====================

    @Test
    @DisplayName("POST /social/invitations/send - Success")
    void testSendInvitation_Success() throws Exception {
        // Arrange
        when(socialService.sendInvitation(eq(USER_ID), any(InvitationSendDTO.class)))
                .thenReturn(invitationResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/social/invitations/send")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invitationSendDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Invitation sent successfully"))
                .andExpect(jsonPath("$.data.invitationId").value(INVITATION_ID))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(socialService, times(1)).sendInvitation(eq(USER_ID), any(InvitationSendDTO.class));
    }

    @Test
    @DisplayName("POST /social/invitations/send - Validation - Missing Recipient")
    void testSendInvitation_Validation_MissingRecipient() throws Exception {
        // Arrange
        invitationSendDTO.setRecipientUsername("");

        // Act & Assert
        mockMvc.perform(post("/social/invitations/send")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invitationSendDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(socialService, never()).sendInvitation(anyString(), any());
    }

    @Test
    @DisplayName("POST /social/invitations/send - Validation - Missing Board ID")
    void testSendInvitation_Validation_MissingBoardId() throws Exception {
        // Arrange
        invitationSendDTO.setBoardId("");

        // Act & Assert
        mockMvc.perform(post("/social/invitations/send")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invitationSendDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(socialService, never()).sendInvitation(anyString(), any());
    }

    @Test
    @DisplayName("GET /social/invitations - Success")
    void testGetInvitations_Success() throws Exception {
        // Arrange
        when(socialService.getInvitations(USER_ID, null, 0, 20))
                .thenReturn(invitationsPaginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/social/invitations")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Invitations retrieved successfully"))
                .andExpect(jsonPath("$.data.data", hasSize(1)));

        verify(socialService, times(1)).getInvitations(USER_ID, null, 0, 20);
    }

    @Test
    @DisplayName("GET /social/invitations - Success - With Status Filter")
    void testGetInvitations_Success_WithStatusFilter() throws Exception {
        // Arrange
        when(socialService.getInvitations(USER_ID, "PENDING", 0, 20))
                .thenReturn(invitationsPaginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/social/invitations")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("status", "PENDING"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(socialService, times(1)).getInvitations(USER_ID, "PENDING", 0, 20);
    }

    @Test
    @DisplayName("GET /social/invitations/{invitationId} - Success")
    void testGetInvitationDetails_Success() throws Exception {
        // Arrange
        when(socialService.getInvitationDetails(INVITATION_ID, USER_ID))
                .thenReturn(invitationResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/social/invitations/{invitationId}", INVITATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.invitationId").value(INVITATION_ID));

        verify(socialService, times(1)).getInvitationDetails(INVITATION_ID, USER_ID);
    }

    @Test
    @DisplayName("GET /social/invitations/{invitationId} - Failure - Not Found")
    void testGetInvitationDetails_Failure_NotFound() throws Exception {
        // Arrange
        when(socialService.getInvitationDetails(INVITATION_ID, USER_ID))
                .thenThrow(new InvitationNotFoundException("Invitation not found"));

        // Act & Assert
        mockMvc.perform(get("/social/invitations/{invitationId}", INVITATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(socialService, times(1)).getInvitationDetails(INVITATION_ID, USER_ID);
    }

    @Test
    @DisplayName("PUT /social/invitations/{invitationId} - Success - Accept")
    void testRespondToInvitation_Success_Accept() throws Exception {
        // Arrange
        doNothing().when(socialService).respondToInvitation(INVITATION_ID, USER_ID, "accept");

        // Act & Assert
        mockMvc.perform(put("/social/invitations/{invitationId}", INVITATION_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invitationActionDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Invitation accepted successfully"));

        verify(socialService, times(1)).respondToInvitation(INVITATION_ID, USER_ID, "accept");
    }

    @Test
    @DisplayName("PUT /social/invitations/{invitationId} - Success - Decline")
    void testRespondToInvitation_Success_Decline() throws Exception {
        // Arrange
        invitationActionDTO.setAction("decline");
        doNothing().when(socialService).respondToInvitation(INVITATION_ID, USER_ID, "decline");

        // Act & Assert
        mockMvc.perform(put("/social/invitations/{invitationId}", INVITATION_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invitationActionDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation declineed successfully"));

        verify(socialService, times(1)).respondToInvitation(INVITATION_ID, USER_ID, "decline");
    }

    @Test
    @DisplayName("PUT /social/invitations/{invitationId} - Validation - Invalid Action")
    void testRespondToInvitation_Validation_InvalidAction() throws Exception {
        // Arrange
        invitationActionDTO.setAction("invalid");

        // Act & Assert
        mockMvc.perform(put("/social/invitations/{invitationId}", INVITATION_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invitationActionDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(socialService, never()).respondToInvitation(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("DELETE /social/invitations/{invitationId} - Success")
    void testCancelInvitation_Success() throws Exception {
        // Arrange
        doNothing().when(socialService).cancelInvitation(INVITATION_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/social/invitations/{invitationId}", INVITATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Invitation cancelled successfully"));

        verify(socialService, times(1)).cancelInvitation(INVITATION_ID, USER_ID);
    }

    @Test
    @DisplayName("DELETE /social/invitations/{invitationId} - Failure - Unauthorized")
    void testCancelInvitation_Failure_Unauthorized() throws Exception {
        // Arrange
        doThrow(new UnauthorizedAccessException("Only the invitation sender can cancel it"))
                .when(socialService).cancelInvitation(INVITATION_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/social/invitations/{invitationId}", INVITATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(socialService, times(1)).cancelInvitation(INVITATION_ID, USER_ID);
    }

    // ==================== BLOCK/UNBLOCK TESTS ====================

    @Test
    @DisplayName("POST /social/block/{userId} - Success")
    void testBlockUser_Success() throws Exception {
        // Arrange
        doNothing().when(socialService).blockUser(USER_ID, TARGET_USER_ID);

        // Act & Assert
        mockMvc.perform(post("/social/block/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User blocked successfully"));

        verify(socialService, times(1)).blockUser(USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("POST /social/block/{userId} - Failure - Self Block")
    void testBlockUser_Failure_SelfBlock() throws Exception {
        // Arrange
        doThrow(new SelfFollowException("You cannot block yourself"))
                .when(socialService).blockUser(USER_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(post("/social/block/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(socialService, times(1)).blockUser(USER_ID, USER_ID);
    }

    @Test
    @DisplayName("DELETE /social/unblock/{userId} - Success")
    void testUnblockUser_Success() throws Exception {
        // Arrange
        doNothing().when(socialService).unblockUser(USER_ID, TARGET_USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/social/unblock/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User unblocked successfully"));

        verify(socialService, times(1)).unblockUser(USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("DELETE /social/unblock/{userId} - Failure - Not Blocked")
    void testUnblockUser_Failure_NotBlocked() throws Exception {
        // Arrange
        doThrow(new NotFollowingException("User is not blocked"))
                .when(socialService).unblockUser(USER_ID, TARGET_USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/social/unblock/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(socialService, times(1)).unblockUser(USER_ID, TARGET_USER_ID);
    }

    // ==================== REPORT USER TESTS ====================

    @Test
    @DisplayName("POST /social/report/{userId} - Success")
    void testReportUser_Success() throws Exception {
        // Arrange
        doNothing().when(socialService).reportUser(eq(USER_ID), eq(TARGET_USER_ID), any(UserReportDTO.class));

        // Act & Assert
        mockMvc.perform(post("/social/report/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReportDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Report submitted successfully"));

        verify(socialService, times(1)).reportUser(eq(USER_ID), eq(TARGET_USER_ID), any(UserReportDTO.class));
    }

    @Test
    @DisplayName("POST /social/report/{userId} - Validation - Missing Reason")
    void testReportUser_Validation_MissingReason() throws Exception {
        // Arrange
        userReportDTO.setReason("");

        // Act & Assert
        mockMvc.perform(post("/social/report/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReportDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(socialService, never()).reportUser(anyString(), anyString(), any());
    }

    // ==================== IS FOLLOWING TESTS ====================

    @Test
    @DisplayName("GET /social/is-following/{userId} - Success - True")
    void testIsFollowing_Success_True() throws Exception {
        // Arrange
        when(socialService.isFollowing(USER_ID, TARGET_USER_ID))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/social/is-following/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Status retrieved"))
                .andExpect(jsonPath("$.data").value(true));

        verify(socialService, times(1)).isFollowing(USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("GET /social/is-following/{userId} - Success - False")
    void testIsFollowing_Success_False() throws Exception {
        // Arrange
        when(socialService.isFollowing(USER_ID, TARGET_USER_ID))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/social/is-following/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        verify(socialService, times(1)).isFollowing(USER_ID, TARGET_USER_ID);
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    @DisplayName("Complete Follow Workflow - Follow, Check Status, Unfollow")
    void testCompleteFollowWorkflow() throws Exception {
        // Arrange
        doNothing().when(socialService).followUser(USER_ID, TARGET_USER_ID);
        when(socialService.isFollowing(USER_ID, TARGET_USER_ID)).thenReturn(true);
        doNothing().when(socialService).unfollowUser(USER_ID, TARGET_USER_ID);

        // Follow
        mockMvc.perform(post("/social/follow/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Check status
        mockMvc.perform(get("/social/is-following/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // Unfollow
        mockMvc.perform(delete("/social/unfollow/{userId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        verify(socialService, times(1)).followUser(USER_ID, TARGET_USER_ID);
        verify(socialService, times(1)).isFollowing(USER_ID, TARGET_USER_ID);
        verify(socialService, times(1)).unfollowUser(USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("Multiple Followers - Get Followers with Multiple Results")
    void testMultipleFollowers() throws Exception {
        // Arrange
        FollowerResponseDTO follower2 = new FollowerResponseDTO();
        follower2.setUserId("user-789");
        follower2.setUsername("follower2");
        follower2.setIsFollowing(false);

        PaginationDTO pagination = new PaginationDTO(0, 1, 2L, 20, false, false);
        PaginatedResponse<FollowerResponseDTO> response = new PaginatedResponse<>(
                Arrays.asList(followerResponseDTO, follower2),
                pagination
        );

        when(socialService.getFollowers(USER_ID, USER_ID, 0, 20))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/social/followers/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(2));

        verify(socialService, times(1)).getFollowers(USER_ID, USER_ID, 0, 20);
    }
}
