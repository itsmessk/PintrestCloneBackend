package com.infy.pinterest.service;



import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; 
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.pinterest.dto.BoardSummaryDTO;
import com.infy.pinterest.dto.FollowStatsDTO;
import com.infy.pinterest.dto.FollowerResponseDTO;
import com.infy.pinterest.dto.FollowingResponseDTO;
import com.infy.pinterest.dto.InvitationResponseDTO;
import com.infy.pinterest.dto.InvitationSendDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PaginationDTO;
import com.infy.pinterest.dto.UserReportDTO;
import com.infy.pinterest.dto.UserSummaryDTO;
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

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SocialService {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardCollaboratorRepository collaboratorRepository;

    @Autowired
    private BlockedUserRepository blockedUserRepository;

    @Autowired
    private UserReportRepository userReportRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public void followUser(String followerId, String followingId) {
        log.info("User {} following user {}", followerId, followingId);

        if (followerId.equals(followingId)) {
            throw new SelfFollowException("You cannot follow yourself");
        }

        userRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("Follower user not found"));
        userRepository.findById(followingId)
                .orElseThrow(() -> new ResourceNotFoundException("Following user not found"));

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new AlreadyFollowingException("You are already following this user");
        }

        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        followRepository.save(follow);
        
        // Create notification for the followed user
        User follower = userRepository.findById(followerId).orElse(null);
        String message = follower != null 
            ? follower.getUsername() + " started following you"
            : "You have a new follower";
        
        notificationService.createNotification(
            followingId,
            followerId,
            Notification.NotificationType.NEW_FOLLOWER,
            message,
            follow.getFollowId(),
            "FOLLOW"
        );

        log.info("User {} now follows user {}", followerId, followingId);
    }

    @Transactional
    public void unfollowUser(String followerId, String followingId) {
        log.info("User {} unfollowing user {}", followerId, followingId);

        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new NotFollowingException("You are not following this user");
        }

        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        log.info("User {} unfollowed user {}", followerId, followingId);
    }

    public PaginatedResponse<FollowerResponseDTO> getFollowers(String userId, String currentUserId, int page, int size) {
        log.info("Fetching followers for user: {}", userId);

        Sort sort = Sort.by(Sort.Direction.DESC, "followedAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Follow> followPage = followRepository.findByFollowingId(userId, pageable);

        List<FollowerResponseDTO> followers = followPage.getContent().stream()
                .map(follow -> {
                    User follower = userRepository.findById(follow.getFollowerId()).orElse(null);
                    if (follower == null) return null;

                    FollowerResponseDTO dto = new FollowerResponseDTO();
                    dto.setUserId(follower.getUserId());
                    dto.setUsername(follower.getUsername());
                    dto.setFullName(follower.getFullName());
                    dto.setProfilePictureUrl(follower.getProfilePictureUrl());
                    dto.setBio(follower.getBio());
                    dto.setFollowedAt(follow.getFollowedAt());
                    dto.setIsFollowing(followRepository.existsByFollowerIdAndFollowingId(currentUserId, follower.getUserId()));

                    return dto;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        PaginationDTO pagination = new PaginationDTO(
                followPage.getNumber(),
                followPage.getTotalPages(),
                followPage.getTotalElements(),
                followPage.getSize(),
                followPage.hasNext(),
                followPage.hasPrevious()
        );

        return new PaginatedResponse<>(followers, pagination);
    }

    public PaginatedResponse<FollowingResponseDTO> getFollowing(String userId, String currentUserId, int page, int size) {
        log.info("Fetching following for user: {}", userId);

        Sort sort = Sort.by(Sort.Direction.DESC, "followedAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Follow> followPage = followRepository.findByFollowerId(userId, pageable);

        List<FollowingResponseDTO> following = followPage.getContent().stream()
                .map(follow -> {
                    User user = userRepository.findById(follow.getFollowingId()).orElse(null);
                    if (user == null) return null;

                    FollowingResponseDTO dto = new FollowingResponseDTO();
                    dto.setUserId(user.getUserId());
                    dto.setUsername(user.getUsername());
                    dto.setFullName(user.getFullName());
                    dto.setProfilePictureUrl(user.getProfilePictureUrl());
                    dto.setBio(user.getBio());
                    dto.setFollowedAt(follow.getFollowedAt());
                    dto.setIsFollower(followRepository.existsByFollowerIdAndFollowingId(user.getUserId(), currentUserId));

                    return dto;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        PaginationDTO pagination = new PaginationDTO(
                followPage.getNumber(),
                followPage.getTotalPages(),
                followPage.getTotalElements(),
                followPage.getSize(),
                followPage.hasNext(),
                followPage.hasPrevious()
        );

        return new PaginatedResponse<>(following, pagination);
    }

    public FollowStatsDTO getFollowStats(String userId) {
        Long followers = followRepository.countByFollowingId(userId);
        Long following = followRepository.countByFollowerId(userId);
        return new FollowStatsDTO(followers.intValue(), following.intValue());
    }

    @Transactional
    public InvitationResponseDTO sendInvitation(String fromUserId, InvitationSendDTO invitationDTO) {
        log.info("Sending invitation from {} to {}", fromUserId, invitationDTO.getRecipientUsername());

        userRepository.findById(fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender user not found"));
        User recipient = userRepository.findByUsername(invitationDTO.getRecipientUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found"));

        Board board = boardRepository.findById(invitationDTO.getBoardId())
                .orElseThrow(() -> new BoardNotFoundException("Board not found"));

        if (!board.getUserId().equals(fromUserId)) {
            throw new UnauthorizedAccessException("You don't have permission to invite users to this board");
        }

        invitationRepository.findByBoardIdAndToUserIdAndStatus(
                invitationDTO.getBoardId(),
                recipient.getUserId(),
                Invitation.Status.PENDING
        ).ifPresent(inv -> {
            throw new AlreadyFollowingException("Invitation already sent to this user");
        });

        Invitation invitation = new Invitation();
        invitation.setBoardId(invitationDTO.getBoardId());
        invitation.setFromUserId(fromUserId);
        invitation.setToUserId(recipient.getUserId());
        invitation.setMessage(invitationDTO.getMessage());
        invitation.setPermission(Invitation.Permission.valueOf(invitationDTO.getPermission().toUpperCase()));
        invitation.setStatus(Invitation.Status.PENDING);

        Invitation savedInvitation = invitationRepository.save(invitation);
        
        // Create notification for recipient
        User inviter = userRepository.findById(fromUserId).orElse(null);
        String message = inviter != null 
            ? inviter.getUsername() + " invited you to collaborate on a board"
            : "You have a new board collaboration invitation";
        
        notificationService.createNotification(
            recipient.getUserId(), 
            fromUserId, 
            Notification.NotificationType.INVITATION_RECEIVED,
            message,
            savedInvitation.getInvitationId(),
            "INVITATION"
        );
        
        return buildInvitationResponse(savedInvitation);
    }
    public PaginatedResponse<InvitationResponseDTO> getInvitations(String userId, String status, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Invitation> invitationPage;

        if (status != null && !status.isEmpty()) {
            Invitation.Status statusEnum = Invitation.Status.valueOf(status.toUpperCase());
            invitationPage = invitationRepository.findByToUserIdAndStatus(userId, statusEnum, pageable);
        } else {
            invitationPage = invitationRepository.findByToUserId(userId, pageable);
        }

        List<InvitationResponseDTO> invitations = invitationPage.getContent().stream()
                .map(this::buildInvitationResponse)
                .collect(Collectors.toList());

        PaginationDTO pagination = new PaginationDTO(
                invitationPage.getNumber(),
                invitationPage.getTotalPages(),
                invitationPage.getTotalElements(),
                invitationPage.getSize(),
                invitationPage.hasNext(),
                invitationPage.hasPrevious()
        );

        return new PaginatedResponse<>(invitations, pagination);
    }

    @Transactional
    public void respondToInvitation(String invitationId, String userId, String action) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found"));

        if (!invitation.getToUserId().equals(userId)) {
            throw new UnauthorizedAccessException("This invitation is not for you");
        }

        if ("accept".equalsIgnoreCase(action)) {
            invitation.setStatus(Invitation.Status.ACCEPTED);
            invitation.setRespondedAt(LocalDateTime.now());

            BoardCollaborator collaborator = new BoardCollaborator();
            collaborator.setBoardId(invitation.getBoardId());
            collaborator.setUserId(userId);
            collaborator.setPermission(invitation.getPermission());
            collaboratorRepository.save(collaborator);

            boardRepository.findById(invitation.getBoardId()).ifPresent(board -> {
                board.setIsCollaborative(true);
                boardRepository.save(board);
            });
            
            // Notify the inviter that invitation was accepted
            User accepter = userRepository.findById(userId).orElse(null);
            Board board = boardRepository.findById(invitation.getBoardId()).orElse(null);
            String message = accepter != null && board != null
                ? accepter.getUsername() + " accepted your invitation to collaborate on \"" + board.getName() + "\""
                : "Your invitation was accepted";
            
            notificationService.createNotification(
                invitation.getFromUserId(),
                userId,
                Notification.NotificationType.INVITATION_ACCEPTED,
                message,
                invitation.getInvitationId(),
                "INVITATION"
            );
        } else if ("decline".equalsIgnoreCase(action)) {
            invitation.setStatus(Invitation.Status.DECLINED);
            invitation.setRespondedAt(LocalDateTime.now());
            
            // Delete notification - no further notifications after decline
            notificationService.deleteNotificationsByEntity(
                invitation.getInvitationId(), 
                Notification.NotificationType.INVITATION_RECEIVED
            );
        } else if ("ignore".equalsIgnoreCase(action)) {
            invitation.setStatus(Invitation.Status.IGNORED);
            invitation.setRespondedAt(LocalDateTime.now());
            
            // Delete notification - no further notifications after ignore
            notificationService.deleteNotificationsByEntity(
                invitation.getInvitationId(), 
                Notification.NotificationType.INVITATION_RECEIVED
            );
        }

        invitationRepository.save(invitation);
    }

    public InvitationResponseDTO getInvitationDetails(String invitationId, String userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found"));

        // User must be either the sender or recipient
        if (!invitation.getFromUserId().equals(userId) && !invitation.getToUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You don't have permission to view this invitation");
        }

        return buildInvitationResponse(invitation);
    }

    @Transactional
    public void cancelInvitation(String invitationId, String userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found"));

        // Only the sender (board owner) can cancel
        if (!invitation.getFromUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Only the invitation sender can cancel it");
        }

        // Can only cancel pending invitations
        if (invitation.getStatus() != Invitation.Status.PENDING) {
            throw new RuntimeException("Cannot cancel invitation with status: " + invitation.getStatus());
        }

        invitationRepository.delete(invitation);
    }

    @Transactional
    public void blockUser(String blockerId, String blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new SelfFollowException("You cannot block yourself");
        }

        userRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocker user not found"));
        userRepository.findById(blockedId)
                .orElseThrow(() -> new ResourceNotFoundException("User to block not found"));

        if (blockedUserRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new AlreadyFollowingException("User is already blocked");
        }

        BlockedUser blockedUser = new BlockedUser();
        blockedUser.setBlockerId(blockerId);
        blockedUser.setBlockedId(blockedId);
        blockedUserRepository.save(blockedUser);

        if (followRepository.existsByFollowerIdAndFollowingId(blockerId, blockedId)) {
            followRepository.deleteByFollowerIdAndFollowingId(blockerId, blockedId);
        }
        if (followRepository.existsByFollowerIdAndFollowingId(blockedId, blockerId)) {
            followRepository.deleteByFollowerIdAndFollowingId(blockedId, blockerId);
        }
    }

    @Transactional
    public void unblockUser(String blockerId, String blockedId) {
        if (!blockedUserRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new NotFollowingException("User is not blocked");
        }
        blockedUserRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional
    public void reportUser(String reporterId, String reportedUserId, UserReportDTO reportDTO) {
        userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter user not found"));
        userRepository.findById(reportedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Reported user not found"));

        UserReport report = new UserReport();
        report.setReporterId(reporterId);
        report.setReportedUserId(reportedUserId);
        report.setReason(UserReport.Reason.valueOf(reportDTO.getReason().toUpperCase()));
        report.setDescription(reportDTO.getDescription());
        report.setStatus(UserReport.Status.PENDING);
        userReportRepository.save(report);
    }

    public Boolean isFollowing(String followerId, String followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public Boolean isBlocked(String blockerId, String blockedId) {
        return blockedUserRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    private InvitationResponseDTO buildInvitationResponse(Invitation invitation) {
        InvitationResponseDTO dto = new InvitationResponseDTO();
        dto.setInvitationId(invitation.getInvitationId());
        dto.setType("board_collaboration");
        dto.setMessage(invitation.getMessage());
        dto.setPermission(invitation.getPermission().name());
        dto.setStatus(invitation.getStatus().name());
        dto.setSentAt(invitation.getCreatedAt());

        userRepository.findById(invitation.getFromUserId()).ifPresent(user -> {
            UserSummaryDTO userSummary = new UserSummaryDTO();
            userSummary.setUserId(user.getUserId());
            userSummary.setUsername(user.getUsername());
            userSummary.setProfilePictureUrl(user.getProfilePictureUrl());
            dto.setFrom(userSummary);
        });

        boardRepository.findById(invitation.getBoardId()).ifPresent(board -> {
            BoardSummaryDTO boardSummary = new BoardSummaryDTO();
            boardSummary.setBoardId(board.getBoardId());
            boardSummary.setBoardName(board.getName());
            dto.setBoard(boardSummary);
        });

        return dto;
    }
}
