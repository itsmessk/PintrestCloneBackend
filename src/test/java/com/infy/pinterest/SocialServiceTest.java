package com.infy.pinterest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.infy.pinterest.dto.FollowStatsDTO;
import com.infy.pinterest.dto.FollowerResponseDTO;
import com.infy.pinterest.dto.FollowingResponseDTO;
import com.infy.pinterest.dto.InvitationResponseDTO;
import com.infy.pinterest.dto.InvitationSendDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.UserReportDTO;
import com.infy.pinterest.entity.BlockedUser;
import com.infy.pinterest.entity.Board;
import com.infy.pinterest.entity.BoardCollaborator;
import com.infy.pinterest.entity.Follow;
import com.infy.pinterest.entity.Invitation;
import com.infy.pinterest.entity.Notification;
import com.infy.pinterest.entity.User;
import com.infy.pinterest.entity.UserReport;
import com.infy.pinterest.exception.AlreadyFollowingException;
import com.infy.pinterest.exception.BoardNotFoundException;
import com.infy.pinterest.exception.InvitationNotFoundException;
import com.infy.pinterest.exception.NotFollowingException;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.exception.SelfFollowException;
import com.infy.pinterest.exception.UnauthorizedAccessException;
import com.infy.pinterest.repository.BlockedUserRepository;
import com.infy.pinterest.repository.BoardCollaboratorRepository;
import com.infy.pinterest.repository.BoardRepository;
import com.infy.pinterest.repository.FollowRepository;
import com.infy.pinterest.repository.InvitationRepository;
import com.infy.pinterest.repository.UserReportRepository;
import com.infy.pinterest.repository.UserRepository;
import com.infy.pinterest.service.NotificationService;
import com.infy.pinterest.service.SocialService;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardCollaboratorRepository collaboratorRepository;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private UserReportRepository userReportRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SocialService socialService;

    private User user1;
    private User user2;
    private Follow follow;
    private Board board;
    private Invitation invitation;

    @BeforeEach
    void setUp() {
        // Setup user1
        user1 = new User();
        user1.setUserId("user-001");
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setFullName("User One");
        user1.setProfilePictureUrl("https://example.com/user1.jpg");
        user1.setBio("Bio for user 1");
        user1.setIsActive(true);

        // Setup user2
        user2 = new User();
        user2.setUserId("user-002");
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setFullName("User Two");
        user2.setProfilePictureUrl("https://example.com/user2.jpg");
        user2.setBio("Bio for user 2");
        user2.setIsActive(true);

        // Setup follow
        follow = new Follow();
        follow.setFollowId("follow-001");
        follow.setFollowerId("user-001");
        follow.setFollowingId("user-002");
        follow.setFollowedAt(LocalDateTime.now());

        // Setup board
        board = new Board();
        board.setBoardId("board-001");
        board.setUserId("user-001");
        board.setName("Test Board");
        board.setVisibility(Board.Visibility.PUBLIC);
        board.setIsCollaborative(false);

        // Setup invitation
        invitation = new Invitation();
        invitation.setInvitationId("invitation-001");
        invitation.setBoardId("board-001");
        invitation.setFromUserId("user-001");
        invitation.setToUserId("user-002");
        invitation.setMessage("Join my board!");
        invitation.setPermission(Invitation.Permission.EDIT);
        invitation.setStatus(Invitation.Status.PENDING);
        invitation.setCreatedAt(LocalDateTime.now());
    }

    // ==================== FOLLOW USER TESTS ====================

    @Test
    void testFollowUser_Success() {
        // Arrange
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(false);
        when(followRepository.save(any(Follow.class))).thenReturn(follow);
        doNothing().when(notificationService).createNotification(anyString(), anyString(), any(), anyString(), anyString(), anyString());

        // Act
        socialService.followUser("user-001", "user-002");

        // Assert
        verify(followRepository).save(any(Follow.class));
        verify(notificationService).createNotification(
            eq("user-002"), 
            eq("user-001"), 
            eq(Notification.NotificationType.NEW_FOLLOWER), 
            anyString(), 
            anyString(), 
            eq("FOLLOW")
        );
    }

    @Test
    void testFollowUser_SelfFollow() {
        // Act & Assert
        SelfFollowException exception = assertThrows(SelfFollowException.class, () -> {
            socialService.followUser("user-001", "user-001");
        });

        assertEquals("You cannot follow yourself", exception.getMessage());
        verify(followRepository, never()).save(any(Follow.class));
    }

    @Test
    void testFollowUser_FollowerNotFound() {
        // Arrange
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            socialService.followUser("non-existent", "user-002");
        });

        assertEquals("Follower user not found", exception.getMessage());
        verify(followRepository, never()).save(any(Follow.class));
    }

    @Test
    void testFollowUser_FollowingUserNotFound() {
        // Arrange
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            socialService.followUser("user-001", "non-existent");
        });

        assertEquals("Following user not found", exception.getMessage());
        verify(followRepository, never()).save(any(Follow.class));
    }

    @Test
    void testFollowUser_AlreadyFollowing() {
        // Arrange
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(true);

        // Act & Assert
        AlreadyFollowingException exception = assertThrows(AlreadyFollowingException.class, () -> {
            socialService.followUser("user-001", "user-002");
        });

        assertEquals("You are already following this user", exception.getMessage());
        verify(followRepository, never()).save(any(Follow.class));
    }

    @Test
    void testFollowUser_NotificationWithUsername() {
        // Arrange
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(false);
        when(followRepository.save(any(Follow.class))).thenReturn(follow);
        doNothing().when(notificationService).createNotification(anyString(), anyString(), any(), anyString(), anyString(), anyString());

        // Act
        socialService.followUser("user-001", "user-002");

        // Assert
        verify(notificationService).createNotification(
            eq("user-002"), 
            eq("user-001"), 
            eq(Notification.NotificationType.NEW_FOLLOWER), 
            eq("user1 started following you"), 
            anyString(), 
            eq("FOLLOW")
        );
    }

    // ==================== UNFOLLOW USER TESTS ====================

    @Test
    void testUnfollowUser_Success() {
        // Arrange
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(true);
        doNothing().when(followRepository).deleteByFollowerIdAndFollowingId("user-001", "user-002");

        // Act
        socialService.unfollowUser("user-001", "user-002");

        // Assert
        verify(followRepository).deleteByFollowerIdAndFollowingId("user-001", "user-002");
    }

    @Test
    void testUnfollowUser_NotFollowing() {
        // Arrange
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(false);

        // Act & Assert
        NotFollowingException exception = assertThrows(NotFollowingException.class, () -> {
            socialService.unfollowUser("user-001", "user-002");
        });

        assertEquals("You are not following this user", exception.getMessage());
        verify(followRepository, never()).deleteByFollowerIdAndFollowingId(anyString(), anyString());
    }

    // ==================== GET FOLLOWERS TESTS ====================

    @Test
    void testGetFollowers_Success() {
        // Arrange
        List<Follow> follows = Arrays.asList(follow);
        Page<Follow> followPage = new PageImpl<>(follows, PageRequest.of(0, 10), 1);

        when(followRepository.findByFollowingId(eq("user-002"), any(Pageable.class))).thenReturn(followPage);
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(followRepository.existsByFollowerIdAndFollowingId("current-user", "user-001")).thenReturn(true);

        // Act
        PaginatedResponse<FollowerResponseDTO> result = socialService.getFollowers("user-002", "current-user", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals("user-001", result.getData().get(0).getUserId());
        assertEquals("user1", result.getData().get(0).getUsername());
        assertTrue(result.getData().get(0).getIsFollowing());
    }

    @Test
    void testGetFollowers_EmptyList() {
        // Arrange
        Page<Follow> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(followRepository.findByFollowingId(eq("user-002"), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<FollowerResponseDTO> result = socialService.getFollowers("user-002", "current-user", 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testGetFollowers_UserNotFound() {
        // Arrange
        List<Follow> follows = Arrays.asList(follow);
        Page<Follow> followPage = new PageImpl<>(follows, PageRequest.of(0, 10), 1);

        when(followRepository.findByFollowingId(eq("user-002"), any(Pageable.class))).thenReturn(followPage);
        when(userRepository.findById("user-001")).thenReturn(Optional.empty());

        // Act
        PaginatedResponse<FollowerResponseDTO> result = socialService.getFollowers("user-002", "current-user", 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty()); // Filtered out null
    }

    @Test
    void testGetFollowers_WithPagination() {
        // Arrange
        List<Follow> follows = Arrays.asList(follow);
        Page<Follow> followPage = new PageImpl<>(follows, PageRequest.of(1, 5), 20);

        when(followRepository.findByFollowingId(eq("user-002"), any(Pageable.class))).thenReturn(followPage);
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(followRepository.existsByFollowerIdAndFollowingId("current-user", "user-001")).thenReturn(false);

        // Act
        PaginatedResponse<FollowerResponseDTO> result = socialService.getFollowers("user-002", "current-user", 1, 5);

        // Assert
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(4, result.getPagination().getTotalPages());
        assertEquals(20L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetFollowers_IsFollowingFalse() {
        // Arrange
        List<Follow> follows = Arrays.asList(follow);
        Page<Follow> followPage = new PageImpl<>(follows, PageRequest.of(0, 10), 1);

        when(followRepository.findByFollowingId(eq("user-002"), any(Pageable.class))).thenReturn(followPage);
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(followRepository.existsByFollowerIdAndFollowingId("current-user", "user-001")).thenReturn(false);

        // Act
        PaginatedResponse<FollowerResponseDTO> result = socialService.getFollowers("user-002", "current-user", 0, 10);

        // Assert
        assertFalse(result.getData().get(0).getIsFollowing());
    }

    // ==================== GET FOLLOWING TESTS ====================

    @Test
    void testGetFollowing_Success() {
        // Arrange
        List<Follow> follows = Arrays.asList(follow);
        Page<Follow> followPage = new PageImpl<>(follows, PageRequest.of(0, 10), 1);

        when(followRepository.findByFollowerId(eq("user-001"), any(Pageable.class))).thenReturn(followPage);
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(followRepository.existsByFollowerIdAndFollowingId("user-002", "current-user")).thenReturn(true);

        // Act
        PaginatedResponse<FollowingResponseDTO> result = socialService.getFollowing("user-001", "current-user", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals("user-002", result.getData().get(0).getUserId());
        assertEquals("user2", result.getData().get(0).getUsername());
        assertTrue(result.getData().get(0).getIsFollower());
    }

    @Test
    void testGetFollowing_EmptyList() {
        // Arrange
        Page<Follow> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(followRepository.findByFollowerId(eq("user-001"), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<FollowingResponseDTO> result = socialService.getFollowing("user-001", "current-user", 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testGetFollowing_UserNotFound() {
        // Arrange
        List<Follow> follows = Arrays.asList(follow);
        Page<Follow> followPage = new PageImpl<>(follows, PageRequest.of(0, 10), 1);

        when(followRepository.findByFollowerId(eq("user-001"), any(Pageable.class))).thenReturn(followPage);
        when(userRepository.findById("user-002")).thenReturn(Optional.empty());

        // Act
        PaginatedResponse<FollowingResponseDTO> result = socialService.getFollowing("user-001", "current-user", 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty()); // Filtered out null
    }

    @Test
    void testGetFollowing_WithPagination() {
        // Arrange
        List<Follow> follows = Arrays.asList(follow);
        Page<Follow> followPage = new PageImpl<>(follows, PageRequest.of(2, 5), 30);

        when(followRepository.findByFollowerId(eq("user-001"), any(Pageable.class))).thenReturn(followPage);
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(followRepository.existsByFollowerIdAndFollowingId("user-002", "current-user")).thenReturn(false);

        // Act
        PaginatedResponse<FollowingResponseDTO> result = socialService.getFollowing("user-001", "current-user", 2, 5);

        // Assert
        assertEquals(2, result.getPagination().getCurrentPage());
        assertEquals(6, result.getPagination().getTotalPages());
        assertEquals(30L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetFollowing_IsFollowerFalse() {
        // Arrange
        List<Follow> follows = Arrays.asList(follow);
        Page<Follow> followPage = new PageImpl<>(follows, PageRequest.of(0, 10), 1);

        when(followRepository.findByFollowerId(eq("user-001"), any(Pageable.class))).thenReturn(followPage);
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(followRepository.existsByFollowerIdAndFollowingId("user-002", "current-user")).thenReturn(false);

        // Act
        PaginatedResponse<FollowingResponseDTO> result = socialService.getFollowing("user-001", "current-user", 0, 10);

        // Assert
        assertFalse(result.getData().get(0).getIsFollower());
    }

    // ==================== GET FOLLOW STATS TESTS ====================

    @Test
    void testGetFollowStats_Success() {
        // Arrange
        when(followRepository.countByFollowingId("user-001")).thenReturn(100L);
        when(followRepository.countByFollowerId("user-001")).thenReturn(50L);

        // Act
        FollowStatsDTO result = socialService.getFollowStats("user-001");

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getFollowers());
        assertEquals(50, result.getFollowing());
    }

    @Test
    void testGetFollowStats_ZeroCounts() {
        // Arrange
        when(followRepository.countByFollowingId("user-001")).thenReturn(0L);
        when(followRepository.countByFollowerId("user-001")).thenReturn(0L);

        // Act
        FollowStatsDTO result = socialService.getFollowStats("user-001");

        // Assert
        assertEquals(0, result.getFollowers());
        assertEquals(0, result.getFollowing());
    }

    // ==================== SEND INVITATION TESTS ====================

    @Test
    void testSendInvitation_Success() {
        // Arrange
        InvitationSendDTO inviteDTO = new InvitationSendDTO();
        inviteDTO.setRecipientUsername("user2");
        inviteDTO.setBoardId("board-001");
        inviteDTO.setMessage("Join my board!");
        inviteDTO.setPermission("EDIT");

        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(user2));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(invitationRepository.findByBoardIdAndToUserIdAndStatus("board-001", "user-002", Invitation.Status.PENDING))
            .thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);
        doNothing().when(notificationService).createNotification(anyString(), anyString(), any(), anyString(), anyString(), anyString());

        // Act
        InvitationResponseDTO result = socialService.sendInvitation("user-001", inviteDTO);

        // Assert
        assertNotNull(result);
        assertEquals("board_collaboration", result.getType());
        verify(invitationRepository).save(any(Invitation.class));
        verify(notificationService).createNotification(
            eq("user-002"), 
            eq("user-001"), 
            eq(Notification.NotificationType.INVITATION_RECEIVED), 
            anyString(), 
            anyString(), 
            eq("INVITATION")
        );
    }

    @Test
    void testSendInvitation_SenderNotFound() {
        // Arrange
        InvitationSendDTO inviteDTO = new InvitationSendDTO();
        inviteDTO.setRecipientUsername("user2");
        inviteDTO.setBoardId("board-001");
        inviteDTO.setPermission("EDIT");

        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            socialService.sendInvitation("non-existent", inviteDTO);
        });

        assertEquals("Sender user not found", exception.getMessage());
        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    void testSendInvitation_RecipientNotFound() {
        // Arrange
        InvitationSendDTO inviteDTO = new InvitationSendDTO();
        inviteDTO.setRecipientUsername("nonexistent");
        inviteDTO.setBoardId("board-001");
        inviteDTO.setPermission("EDIT");

        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            socialService.sendInvitation("user-001", inviteDTO);
        });

        assertEquals("Recipient user not found", exception.getMessage());
        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    void testSendInvitation_BoardNotFound() {
        // Arrange
        InvitationSendDTO inviteDTO = new InvitationSendDTO();
        inviteDTO.setRecipientUsername("user2");
        inviteDTO.setBoardId("non-existent");
        inviteDTO.setPermission("EDIT");

        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(user2));
        when(boardRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        BoardNotFoundException exception = assertThrows(BoardNotFoundException.class, () -> {
            socialService.sendInvitation("user-001", inviteDTO);
        });

        assertEquals("Board not found", exception.getMessage());
        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    void testSendInvitation_NotBoardOwner() {
        // Arrange
        InvitationSendDTO inviteDTO = new InvitationSendDTO();
        inviteDTO.setRecipientUsername("user2");
        inviteDTO.setBoardId("board-001");
        inviteDTO.setPermission("EDIT");

        board.setUserId("other-user"); // Different owner

        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(user2));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            socialService.sendInvitation("user-001", inviteDTO);
        });

        assertEquals("You don't have permission to invite users to this board", exception.getMessage());
        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    void testSendInvitation_AlreadyInvited() {
        // Arrange
        InvitationSendDTO inviteDTO = new InvitationSendDTO();
        inviteDTO.setRecipientUsername("user2");
        inviteDTO.setBoardId("board-001");
        inviteDTO.setPermission("EDIT");

        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(user2));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(invitationRepository.findByBoardIdAndToUserIdAndStatus("board-001", "user-002", Invitation.Status.PENDING))
            .thenReturn(Optional.of(invitation));

        // Act & Assert
        AlreadyFollowingException exception = assertThrows(AlreadyFollowingException.class, () -> {
            socialService.sendInvitation("user-001", inviteDTO);
        });

        assertEquals("Invitation already sent to this user", exception.getMessage());
        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    void testSendInvitation_ViewPermission() {
        // Arrange
        InvitationSendDTO inviteDTO = new InvitationSendDTO();
        inviteDTO.setRecipientUsername("user2");
        inviteDTO.setBoardId("board-001");
        inviteDTO.setMessage("View my board!");
        inviteDTO.setPermission("VIEW");

        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(user2));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(invitationRepository.findByBoardIdAndToUserIdAndStatus("board-001", "user-002", Invitation.Status.PENDING))
            .thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);
        doNothing().when(notificationService).createNotification(anyString(), anyString(), any(), anyString(), anyString(), anyString());

        // Act
        InvitationResponseDTO result = socialService.sendInvitation("user-001", inviteDTO);

        // Assert
        assertNotNull(result);
        verify(invitationRepository).save(any(Invitation.class));
    }

    // ==================== GET INVITATIONS TESTS ====================

    @Test
    void testGetInvitations_AllStatuses() {
        // Arrange
        List<Invitation> invitations = Arrays.asList(invitation);
        Page<Invitation> invitationPage = new PageImpl<>(invitations, PageRequest.of(0, 10), 1);

        when(invitationRepository.findByToUserId(eq("user-002"), any(Pageable.class))).thenReturn(invitationPage);
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));

        // Act
        PaginatedResponse<InvitationResponseDTO> result = socialService.getInvitations("user-002", null, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals("invitation-001", result.getData().get(0).getInvitationId());
    }

    @Test
    void testGetInvitations_PendingOnly() {
        // Arrange
        List<Invitation> invitations = Arrays.asList(invitation);
        Page<Invitation> invitationPage = new PageImpl<>(invitations, PageRequest.of(0, 10), 1);

        when(invitationRepository.findByToUserIdAndStatus(eq("user-002"), eq(Invitation.Status.PENDING), any(Pageable.class)))
            .thenReturn(invitationPage);
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));

        // Act
        PaginatedResponse<InvitationResponseDTO> result = socialService.getInvitations("user-002", "PENDING", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals("PENDING", result.getData().get(0).getStatus());
    }

    @Test
    void testGetInvitations_EmptyList() {
        // Arrange
        Page<Invitation> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(invitationRepository.findByToUserId(eq("user-002"), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<InvitationResponseDTO> result = socialService.getInvitations("user-002", "", 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testGetInvitations_WithPagination() {
        // Arrange
        List<Invitation> invitations = Arrays.asList(invitation);
        Page<Invitation> invitationPage = new PageImpl<>(invitations, PageRequest.of(1, 5), 15);

        when(invitationRepository.findByToUserId(eq("user-002"), any(Pageable.class))).thenReturn(invitationPage);
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));

        // Act
        PaginatedResponse<InvitationResponseDTO> result = socialService.getInvitations("user-002", null, 1, 5);

        // Assert
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(3, result.getPagination().getTotalPages());
    }

    // ==================== RESPOND TO INVITATION TESTS ====================

    @Test
    void testRespondToInvitation_Accept() {
        // Arrange
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);
        when(collaboratorRepository.save(any(BoardCollaborator.class))).thenReturn(new BoardCollaborator());
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        doNothing().when(notificationService).createNotification(anyString(), anyString(), any(), anyString(), anyString(), anyString());

        // Act
        socialService.respondToInvitation("invitation-001", "user-002", "accept");

        // Assert
        assertEquals(Invitation.Status.ACCEPTED, invitation.getStatus());
        assertNotNull(invitation.getRespondedAt());
        verify(collaboratorRepository).save(any(BoardCollaborator.class));
        verify(boardRepository).save(any(Board.class));
        assertTrue(board.getIsCollaborative());
        verify(notificationService).createNotification(
            eq("user-001"), 
            eq("user-002"), 
            eq(Notification.NotificationType.INVITATION_ACCEPTED), 
            anyString(), 
            eq("invitation-001"), 
            eq("INVITATION")
        );
    }

    @Test
    void testRespondToInvitation_Decline() {
        // Arrange
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);
        doNothing().when(notificationService).deleteNotificationsByEntity(anyString(), any());

        // Act
        socialService.respondToInvitation("invitation-001", "user-002", "decline");

        // Assert
        assertEquals(Invitation.Status.DECLINED, invitation.getStatus());
        assertNotNull(invitation.getRespondedAt());
        verify(collaboratorRepository, never()).save(any(BoardCollaborator.class));
        verify(notificationService).deleteNotificationsByEntity(
            eq("invitation-001"), 
            eq(Notification.NotificationType.INVITATION_RECEIVED)
        );
    }

    @Test
    void testRespondToInvitation_Ignore() {
        // Arrange
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);
        doNothing().when(notificationService).deleteNotificationsByEntity(anyString(), any());

        // Act
        socialService.respondToInvitation("invitation-001", "user-002", "ignore");

        // Assert
        assertEquals(Invitation.Status.IGNORED, invitation.getStatus());
        assertNotNull(invitation.getRespondedAt());
        verify(collaboratorRepository, never()).save(any(BoardCollaborator.class));
        verify(notificationService).deleteNotificationsByEntity(
            eq("invitation-001"), 
            eq(Notification.NotificationType.INVITATION_RECEIVED)
        );
    }

    @Test
    void testRespondToInvitation_InvitationNotFound() {
        // Arrange
        when(invitationRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        InvitationNotFoundException exception = assertThrows(InvitationNotFoundException.class, () -> {
            socialService.respondToInvitation("non-existent", "user-002", "accept");
        });

        assertEquals("Invitation not found", exception.getMessage());
    }

    @Test
    void testRespondToInvitation_NotRecipient() {
        // Arrange
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            socialService.respondToInvitation("invitation-001", "other-user", "accept");
        });

        assertEquals("This invitation is not for you", exception.getMessage());
    }

    // ==================== GET INVITATION DETAILS TESTS ====================

    @Test
    void testGetInvitationDetails_AsRecipient() {
        // Arrange
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));

        // Act
        InvitationResponseDTO result = socialService.getInvitationDetails("invitation-001", "user-002");

        // Assert
        assertNotNull(result);
        assertEquals("invitation-001", result.getInvitationId());
    }

    @Test
    void testGetInvitationDetails_AsSender() {
        // Arrange
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));

        // Act
        InvitationResponseDTO result = socialService.getInvitationDetails("invitation-001", "user-001");

        // Assert
        assertNotNull(result);
        assertEquals("invitation-001", result.getInvitationId());
    }

    @Test
    void testGetInvitationDetails_NotFound() {
        // Arrange
        when(invitationRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        InvitationNotFoundException exception = assertThrows(InvitationNotFoundException.class, () -> {
            socialService.getInvitationDetails("non-existent", "user-001");
        });

        assertEquals("Invitation not found", exception.getMessage());
    }

    @Test
    void testGetInvitationDetails_Unauthorized() {
        // Arrange
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            socialService.getInvitationDetails("invitation-001", "other-user");
        });

        assertEquals("You don't have permission to view this invitation", exception.getMessage());
    }

    // ==================== CANCEL INVITATION TESTS ====================

    @Test
    void testCancelInvitation_Success() {
        // Arrange
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));
        doNothing().when(invitationRepository).delete(invitation);

        // Act
        socialService.cancelInvitation("invitation-001", "user-001");

        // Assert
        verify(invitationRepository).delete(invitation);
    }

    @Test
    void testCancelInvitation_NotFound() {
        // Arrange
        when(invitationRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        InvitationNotFoundException exception = assertThrows(InvitationNotFoundException.class, () -> {
            socialService.cancelInvitation("non-existent", "user-001");
        });

        assertEquals("Invitation not found", exception.getMessage());
    }

    @Test
    void testCancelInvitation_NotSender() {
        // Arrange
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            socialService.cancelInvitation("invitation-001", "other-user");
        });

        assertEquals("Only the invitation sender can cancel it", exception.getMessage());
    }

    @Test
    void testCancelInvitation_NotPending() {
        // Arrange
        invitation.setStatus(Invitation.Status.ACCEPTED);
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            socialService.cancelInvitation("invitation-001", "user-001");
        });

        assertEquals("Cannot cancel invitation with status: ACCEPTED", exception.getMessage());
    }

    // ==================== BLOCK USER TESTS ====================

    @Test
    void testBlockUser_Success() {
        // Arrange
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-001", "user-002")).thenReturn(false);
        when(blockedUserRepository.save(any(BlockedUser.class))).thenReturn(new BlockedUser());
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFollowingId("user-002", "user-001")).thenReturn(true);
        doNothing().when(followRepository).deleteByFollowerIdAndFollowingId(anyString(), anyString());

        // Act
        socialService.blockUser("user-001", "user-002");

        // Assert
        verify(blockedUserRepository).save(any(BlockedUser.class));
        verify(followRepository).deleteByFollowerIdAndFollowingId("user-001", "user-002");
        verify(followRepository).deleteByFollowerIdAndFollowingId("user-002", "user-001");
    }

    @Test
    void testBlockUser_SelfBlock() {
        // Act & Assert
        SelfFollowException exception = assertThrows(SelfFollowException.class, () -> {
            socialService.blockUser("user-001", "user-001");
        });

        assertEquals("You cannot block yourself", exception.getMessage());
    }

    @Test
    void testBlockUser_BlockerNotFound() {
        // Arrange
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            socialService.blockUser("non-existent", "user-002");
        });

        assertEquals("Blocker user not found", exception.getMessage());
    }

    @Test
    void testBlockUser_BlockedUserNotFound() {
        // Arrange
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            socialService.blockUser("user-001", "non-existent");
        });

        assertEquals("User to block not found", exception.getMessage());
    }

    @Test
    void testBlockUser_AlreadyBlocked() {
        // Arrange
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-001", "user-002")).thenReturn(true);

        // Act & Assert
        AlreadyFollowingException exception = assertThrows(AlreadyFollowingException.class, () -> {
            socialService.blockUser("user-001", "user-002");
        });

        assertEquals("User is already blocked", exception.getMessage());
    }

    @Test
    void testBlockUser_NoFollowRelationship() {
        // Arrange
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-001", "user-002")).thenReturn(false);
        when(blockedUserRepository.save(any(BlockedUser.class))).thenReturn(new BlockedUser());
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(false);
        when(followRepository.existsByFollowerIdAndFollowingId("user-002", "user-001")).thenReturn(false);

        // Act
        socialService.blockUser("user-001", "user-002");

        // Assert
        verify(blockedUserRepository).save(any(BlockedUser.class));
        verify(followRepository, never()).deleteByFollowerIdAndFollowingId(anyString(), anyString());
    }

    // ==================== UNBLOCK USER TESTS ====================

    @Test
    void testUnblockUser_Success() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-001", "user-002")).thenReturn(true);
        doNothing().when(blockedUserRepository).deleteByBlockerIdAndBlockedId("user-001", "user-002");

        // Act
        socialService.unblockUser("user-001", "user-002");

        // Assert
        verify(blockedUserRepository).deleteByBlockerIdAndBlockedId("user-001", "user-002");
    }

    @Test
    void testUnblockUser_NotBlocked() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-001", "user-002")).thenReturn(false);

        // Act & Assert
        NotFollowingException exception = assertThrows(NotFollowingException.class, () -> {
            socialService.unblockUser("user-001", "user-002");
        });

        assertEquals("User is not blocked", exception.getMessage());
    }

    // ==================== REPORT USER TESTS ====================

    @Test
    void testReportUser_Success() {
        // Arrange
        UserReportDTO reportDTO = new UserReportDTO();
        reportDTO.setReason("SPAM");
        reportDTO.setDescription("This user is spamming");

        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(userReportRepository.save(any(UserReport.class))).thenReturn(new UserReport());

        // Act
        socialService.reportUser("user-001", "user-002", reportDTO);

        // Assert
        verify(userReportRepository).save(any(UserReport.class));
    }

    @Test
    void testReportUser_ReporterNotFound() {
        // Arrange
        UserReportDTO reportDTO = new UserReportDTO();
        reportDTO.setReason("SPAM");

        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            socialService.reportUser("non-existent", "user-002", reportDTO);
        });

        assertEquals("Reporter user not found", exception.getMessage());
    }

    @Test
    void testReportUser_ReportedUserNotFound() {
        // Arrange
        UserReportDTO reportDTO = new UserReportDTO();
        reportDTO.setReason("HARASSMENT");

        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            socialService.reportUser("user-001", "non-existent", reportDTO);
        });

        assertEquals("Reported user not found", exception.getMessage());
    }

    @Test
    void testReportUser_DifferentReasons() {
        // Arrange
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(userReportRepository.save(any(UserReport.class))).thenReturn(new UserReport());

        String[] reasons = {"SPAM", "HARASSMENT", "INAPPROPRIATE", "OTHER"};

        for (String reason : reasons) {
            UserReportDTO reportDTO = new UserReportDTO();
            reportDTO.setReason(reason);
            reportDTO.setDescription("Test description");

            // Act
            socialService.reportUser("user-001", "user-002", reportDTO);
        }

        // Assert
        verify(userReportRepository, times(4)).save(any(UserReport.class));
    }

    // ==================== IS FOLLOWING TESTS ====================

    @Test
    void testIsFollowing_True() {
        // Arrange
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(true);

        // Act
        Boolean result = socialService.isFollowing("user-001", "user-002");

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsFollowing_False() {
        // Arrange
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(false);

        // Act
        Boolean result = socialService.isFollowing("user-001", "user-002");

        // Assert
        assertFalse(result);
    }

    // ==================== IS BLOCKED TESTS ====================

    @Test
    void testIsBlocked_True() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-001", "user-002")).thenReturn(true);

        // Act
        Boolean result = socialService.isBlocked("user-001", "user-002");

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsBlocked_False() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-001", "user-002")).thenReturn(false);

        // Act
        Boolean result = socialService.isBlocked("user-001", "user-002");

        // Assert
        assertFalse(result);
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    void testFollowThenUnfollow() {
        // Arrange - Follow
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(false);
        when(followRepository.save(any(Follow.class))).thenReturn(follow);
        doNothing().when(notificationService).createNotification(anyString(), anyString(), any(), anyString(), anyString(), anyString());

        // Act - Follow
        socialService.followUser("user-001", "user-002");

        // Arrange - Unfollow
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(true);
        doNothing().when(followRepository).deleteByFollowerIdAndFollowingId("user-001", "user-002");

        // Act - Unfollow
        socialService.unfollowUser("user-001", "user-002");

        // Assert
        verify(followRepository).save(any(Follow.class));
        verify(followRepository).deleteByFollowerIdAndFollowingId("user-001", "user-002");
    }

    @Test
    void testSendInvitationThenAccept() {
        // Arrange - Send
        InvitationSendDTO inviteDTO = new InvitationSendDTO();
        inviteDTO.setRecipientUsername("user2");
        inviteDTO.setBoardId("board-001");
        inviteDTO.setMessage("Join my board!");
        inviteDTO.setPermission("EDIT");

        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(user2));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(invitationRepository.findByBoardIdAndToUserIdAndStatus("board-001", "user-002", Invitation.Status.PENDING))
            .thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);
        doNothing().when(notificationService).createNotification(anyString(), anyString(), any(), anyString(), anyString(), anyString());

        // Act - Send
        socialService.sendInvitation("user-001", inviteDTO);

        // Arrange - Accept
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));
        when(collaboratorRepository.save(any(BoardCollaborator.class))).thenReturn(new BoardCollaborator());
        when(boardRepository.save(any(Board.class))).thenReturn(board);

        // Act - Accept
        socialService.respondToInvitation("invitation-001", "user-002", "accept");

        // Assert
        verify(invitationRepository, times(2)).save(any(Invitation.class));
        verify(collaboratorRepository).save(any(BoardCollaborator.class));
        assertEquals(Invitation.Status.ACCEPTED, invitation.getStatus());
    }

    @Test
    void testBlockUserRemovesFollows() {
        // Arrange
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-002")).thenReturn(Optional.of(user2));
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-001", "user-002")).thenReturn(false);
        when(blockedUserRepository.save(any(BlockedUser.class))).thenReturn(new BlockedUser());
        when(followRepository.existsByFollowerIdAndFollowingId("user-001", "user-002")).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFollowingId("user-002", "user-001")).thenReturn(true);
        doNothing().when(followRepository).deleteByFollowerIdAndFollowingId(anyString(), anyString());

        // Act
        socialService.blockUser("user-001", "user-002");

        // Assert
        verify(blockedUserRepository).save(any(BlockedUser.class));
        verify(followRepository).deleteByFollowerIdAndFollowingId("user-001", "user-002");
        verify(followRepository).deleteByFollowerIdAndFollowingId("user-002", "user-001");
    }

    @Test
    void testGetFollowers_MultipleUsers() {
        // Arrange
        Follow follow2 = new Follow();
        follow2.setFollowId("follow-002");
        follow2.setFollowerId("user-003");
        follow2.setFollowingId("user-002");

        User user3 = new User();
        user3.setUserId("user-003");
        user3.setUsername("user3");

        List<Follow> follows = Arrays.asList(follow, follow2);
        Page<Follow> followPage = new PageImpl<>(follows, PageRequest.of(0, 10), 2);

        when(followRepository.findByFollowingId(eq("user-002"), any(Pageable.class))).thenReturn(followPage);
        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-003")).thenReturn(Optional.of(user3));
        when(followRepository.existsByFollowerIdAndFollowingId(anyString(), anyString())).thenReturn(false);

        // Act
        PaginatedResponse<FollowerResponseDTO> result = socialService.getFollowers("user-002", "current-user", 0, 10);

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals(2L, result.getPagination().getTotalItems());
    }

    @Test
    void testSendInvitationThenCancel() {
        // Arrange - Send
        InvitationSendDTO inviteDTO = new InvitationSendDTO();
        inviteDTO.setRecipientUsername("user2");
        inviteDTO.setBoardId("board-001");
        inviteDTO.setPermission("EDIT");

        when(userRepository.findById("user-001")).thenReturn(Optional.of(user1));
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(user2));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(invitationRepository.findByBoardIdAndToUserIdAndStatus("board-001", "user-002", Invitation.Status.PENDING))
            .thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);
        doNothing().when(notificationService).createNotification(anyString(), anyString(), any(), anyString(), anyString(), anyString());

        // Act - Send
        socialService.sendInvitation("user-001", inviteDTO);

        // Arrange - Cancel
        when(invitationRepository.findById("invitation-001")).thenReturn(Optional.of(invitation));
        doNothing().when(invitationRepository).delete(invitation);

        // Act - Cancel
        socialService.cancelInvitation("invitation-001", "user-001");

        // Assert
        verify(invitationRepository).save(any(Invitation.class));
        verify(invitationRepository).delete(invitation);
    }
}
