package com.infy.pinterest.service;

import com.infy.pinterest.dto.BlockedUserDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PaginationDTO;
import com.infy.pinterest.entity.BlockedUser;
import com.infy.pinterest.exception.SelfBlockException;
import com.infy.pinterest.exception.UserBlockedException;
import com.infy.pinterest.repository.BlockedUserRepository;
import com.infy.pinterest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockedUserService {

    private final BlockedUserRepository blockedUserRepository;
    private final UserRepository userRepository;

    /**
     * Check if user A has blocked user B
     * @param blockerId The user who might have blocked
     * @param blockedId The user who might be blocked
     * @return true if blockedId is blocked by blockerId
     */
    public boolean isUserBlocked(String blockerId, String blockedId) {
        if (blockerId == null || blockedId == null) {
            return false;
        }
        return blockedUserRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    /**
     * Check if there's a mutual block between two users (either direction)
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return true if either user has blocked the other
     */
    public boolean isBlockedInEitherDirection(String userId1, String userId2) {
        return isUserBlocked(userId1, userId2) || isUserBlocked(userId2, userId1);
    }

    /**
     * Block a user
     * @param blockerId The user doing the blocking
     * @param blockedId The user to be blocked
     * @throws RuntimeException if already blocked or blocking self
     */
    @Transactional
    public void blockUser(String blockerId, String blockedId) {
        log.info("User {} attempting to block user {}", blockerId, blockedId);

        if (blockerId.equals(blockedId)) {
            throw new SelfBlockException("Cannot block yourself");
        }

        if (isUserBlocked(blockerId, blockedId)) {
            throw new UserBlockedException("User is already blocked");
        }

        BlockedUser blockedUser = new BlockedUser();
        blockedUser.setBlockerId(blockerId);
        blockedUser.setBlockedId(blockedId);
        
        blockedUserRepository.save(blockedUser);
        log.info("User {} successfully blocked user {}", blockerId, blockedId);
    }

    /**
     * Unblock a user
     * @param blockerId The user doing the unblocking
     * @param blockedId The user to be unblocked
     * @throws RuntimeException if not currently blocked
     */
    @Transactional
    public void unblockUser(String blockerId, String blockedId) {
        log.info("User {} attempting to unblock user {}", blockerId, blockedId);

        if (!isUserBlocked(blockerId, blockedId)) {
            throw new UserBlockedException("User is not blocked");
        }

        blockedUserRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        log.info("User {} successfully unblocked user {}", blockerId, blockedId);
    }

    /**
     * Validate that there's no block between two users, throw exception if blocked
     * @param currentUserId The current user
     * @param targetUserId The target user
     * @throws RuntimeException if either user has blocked the other
     */
    public void validateNotBlocked(String currentUserId, String targetUserId) {
        if (isUserBlocked(currentUserId, targetUserId)) {
            throw new UserBlockedException("You have blocked this user");
        }
        if (isUserBlocked(targetUserId, currentUserId)) {
            throw new UserBlockedException("This user has blocked you");
        }
    }

    /**
     * Get paginated list of blocked users for a given user
     * @param blockerId The user ID
     * @param page Page number
     * @param size Page size
     * @return Paginated response of blocked users
     */
    public PaginatedResponse<BlockedUserDTO> getBlockedUsers(String blockerId, int page, int size) {
        log.info("Fetching blocked users for user: {}", blockerId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("blockedAt").descending());
        Page<BlockedUser> blockedUsersPage = blockedUserRepository.findByBlockerId(blockerId, pageable);
        
        List<BlockedUserDTO> blockedUsers = blockedUsersPage.getContent().stream()
                .map(blockedUser -> {
                    BlockedUserDTO dto = new BlockedUserDTO();
                    dto.setBlockId(blockedUser.getBlockId());
                    dto.setBlockedUserId(blockedUser.getBlockedId());
                    dto.setBlockedAt(blockedUser.getBlockedAt());
                    
                    // Fetch blocked user details
                    userRepository.findById(blockedUser.getBlockedId()).ifPresent(user -> {
                        dto.setBlockedUsername(user.getUsername());
                        dto.setBlockedUserFullName(user.getFullName());
                        dto.setBlockedUserProfilePicture(user.getProfilePictureUrl());
                    });
                    
                    return dto;
                })
                .toList();
        
        PaginationDTO pagination = new PaginationDTO(
                blockedUsersPage.getNumber(),
                blockedUsersPage.getTotalPages(),
                blockedUsersPage.getTotalElements(),
                blockedUsersPage.getSize(),
                blockedUsersPage.hasNext(),
                blockedUsersPage.hasPrevious()
        );
        
        return new PaginatedResponse<>(blockedUsers, pagination);
    }
}
