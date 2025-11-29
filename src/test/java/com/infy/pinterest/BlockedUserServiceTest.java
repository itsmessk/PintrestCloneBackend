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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.infy.pinterest.dto.BlockedUserDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.entity.BlockedUser;
import com.infy.pinterest.entity.User;
import com.infy.pinterest.repository.BlockedUserRepository;
import com.infy.pinterest.repository.UserRepository;
import com.infy.pinterest.service.BlockedUserService;

@ExtendWith(MockitoExtension.class)
class BlockedUserServiceTest {

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BlockedUserService blockedUserService;

    private User blocker;
    private User blockedUser;
    private BlockedUser blockRelation;

    @BeforeEach
    void setUp() {
        // Setup blocker user
        blocker = new User();
        blocker.setUserId("blocker-123");
        blocker.setUsername("blocker_user");
        blocker.setEmail("blocker@example.com");
        blocker.setFullName("Blocker User");
        blocker.setProfilePictureUrl("https://example.com/blocker.jpg");
        blocker.setIsActive(true);
        blocker.setCreatedAt(LocalDateTime.now());

        // Setup blocked user
        blockedUser = new User();
        blockedUser.setUserId("blocked-456");
        blockedUser.setUsername("blocked_user");
        blockedUser.setEmail("blocked@example.com");
        blockedUser.setFullName("Blocked User");
        blockedUser.setProfilePictureUrl("https://example.com/blocked.jpg");
        blockedUser.setIsActive(true);
        blockedUser.setCreatedAt(LocalDateTime.now());

        // Setup block relation
        blockRelation = new BlockedUser();
        blockRelation.setBlockId("block-789");
        blockRelation.setBlockerId("blocker-123");
        blockRelation.setBlockedId("blocked-456");
        blockRelation.setBlockedAt(LocalDateTime.now());
    }

    // ==================== IS USER BLOCKED TESTS ====================

    @Test
    void testIsUserBlocked_Success_UserIsBlocked() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("blocker-123", "blocked-456"))
                .thenReturn(true);

        // Act
        boolean result = blockedUserService.isUserBlocked("blocker-123", "blocked-456");

        // Assert
        assertTrue(result);
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("blocker-123", "blocked-456");
    }

    @Test
    void testIsUserBlocked_UserNotBlocked() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("blocker-123", "blocked-456"))
                .thenReturn(false);

        // Act
        boolean result = blockedUserService.isUserBlocked("blocker-123", "blocked-456");

        // Assert
        assertFalse(result);
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("blocker-123", "blocked-456");
    }

    @Test
    void testIsUserBlocked_WithNullBlockerId() {
        // Act
        boolean result = blockedUserService.isUserBlocked(null, "blocked-456");

        // Assert
        assertFalse(result);
        verify(blockedUserRepository, never()).existsByBlockerIdAndBlockedId(anyString(), anyString());
    }

    @Test
    void testIsUserBlocked_WithNullBlockedId() {
        // Act
        boolean result = blockedUserService.isUserBlocked("blocker-123", null);

        // Assert
        assertFalse(result);
        verify(blockedUserRepository, never()).existsByBlockerIdAndBlockedId(anyString(), anyString());
    }

    @Test
    void testIsUserBlocked_WithBothNull() {
        // Act
        boolean result = blockedUserService.isUserBlocked(null, null);

        // Assert
        assertFalse(result);
        verify(blockedUserRepository, never()).existsByBlockerIdAndBlockedId(anyString(), anyString());
    }

    // ==================== IS BLOCKED IN EITHER DIRECTION TESTS ====================

    @Test
    void testIsBlockedInEitherDirection_User1BlockedUser2() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(true);

        // Act
        boolean result = blockedUserService.isBlockedInEitherDirection("user-1", "user-2");

        // Assert
        assertTrue(result);
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("user-1", "user-2");
    }

    @Test
    void testIsBlockedInEitherDirection_User2BlockedUser1() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(false);
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-2", "user-1"))
                .thenReturn(true);

        // Act
        boolean result = blockedUserService.isBlockedInEitherDirection("user-1", "user-2");

        // Assert
        assertTrue(result);
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("user-1", "user-2");
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("user-2", "user-1");
    }

    @Test
    void testIsBlockedInEitherDirection_MutualBlock() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(true);

        // Act
        boolean result = blockedUserService.isBlockedInEitherDirection("user-1", "user-2");

        // Assert
        assertTrue(result);
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("user-1", "user-2");
    }

    @Test
    void testIsBlockedInEitherDirection_NoBlock() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(false);
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-2", "user-1"))
                .thenReturn(false);

        // Act
        boolean result = blockedUserService.isBlockedInEitherDirection("user-1", "user-2");

        // Assert
        assertFalse(result);
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("user-1", "user-2");
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("user-2", "user-1");
    }

    // ==================== BLOCK USER TESTS ====================

    @Test
    void testBlockUser_Success() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("blocker-123", "blocked-456"))
                .thenReturn(false);
        when(blockedUserRepository.save(any(BlockedUser.class))).thenReturn(blockRelation);

        // Act
        blockedUserService.blockUser("blocker-123", "blocked-456");

        // Assert
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("blocker-123", "blocked-456");
        verify(blockedUserRepository).save(any(BlockedUser.class));
    }

    @Test
    void testBlockUser_CannotBlockYourself() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            blockedUserService.blockUser("user-123", "user-123");
        });

        assertEquals("Cannot block yourself", exception.getMessage());
        verify(blockedUserRepository, never()).save(any(BlockedUser.class));
    }

    @Test
    void testBlockUser_AlreadyBlocked() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("blocker-123", "blocked-456"))
                .thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            blockedUserService.blockUser("blocker-123", "blocked-456");
        });

        assertEquals("User is already blocked", exception.getMessage());
        verify(blockedUserRepository, never()).save(any(BlockedUser.class));
    }

    @Test
    void testBlockUser_CreateNewBlockRelation() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("new-blocker", "new-blocked"))
                .thenReturn(false);
        when(blockedUserRepository.save(any(BlockedUser.class))).thenReturn(blockRelation);

        // Act
        blockedUserService.blockUser("new-blocker", "new-blocked");

        // Assert
        verify(blockedUserRepository).save(any(BlockedUser.class));
    }

    // ==================== UNBLOCK USER TESTS ====================

    @Test
    void testUnblockUser_Success() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("blocker-123", "blocked-456"))
                .thenReturn(true);
        doNothing().when(blockedUserRepository).deleteByBlockerIdAndBlockedId("blocker-123", "blocked-456");

        // Act
        blockedUserService.unblockUser("blocker-123", "blocked-456");

        // Assert
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("blocker-123", "blocked-456");
        verify(blockedUserRepository).deleteByBlockerIdAndBlockedId("blocker-123", "blocked-456");
    }

    @Test
    void testUnblockUser_UserNotBlocked() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("blocker-123", "blocked-456"))
                .thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            blockedUserService.unblockUser("blocker-123", "blocked-456");
        });

        assertEquals("User is not blocked", exception.getMessage());
        verify(blockedUserRepository, never()).deleteByBlockerIdAndBlockedId(anyString(), anyString());
    }

    @Test
    void testUnblockUser_RemovesBlockRelation() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(true);
        doNothing().when(blockedUserRepository).deleteByBlockerIdAndBlockedId("user-1", "user-2");

        // Act
        blockedUserService.unblockUser("user-1", "user-2");

        // Assert
        verify(blockedUserRepository).deleteByBlockerIdAndBlockedId("user-1", "user-2");
    }

    // ==================== VALIDATE NOT BLOCKED TESTS ====================

    @Test
    void testValidateNotBlocked_Success_NoBlock() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(false);
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-2", "user-1"))
                .thenReturn(false);

        // Act & Assert (should not throw exception)
        blockedUserService.validateNotBlocked("user-1", "user-2");

        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("user-1", "user-2");
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("user-2", "user-1");
    }

    @Test
    void testValidateNotBlocked_CurrentUserBlockedTarget() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            blockedUserService.validateNotBlocked("user-1", "user-2");
        });

        assertEquals("You have blocked this user", exception.getMessage());
    }

    @Test
    void testValidateNotBlocked_TargetUserBlockedCurrent() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(false);
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-2", "user-1"))
                .thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            blockedUserService.validateNotBlocked("user-1", "user-2");
        });

        assertEquals("This user has blocked you", exception.getMessage());
    }

    @Test
    void testValidateNotBlocked_MutualBlock() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            blockedUserService.validateNotBlocked("user-1", "user-2");
        });

        assertEquals("You have blocked this user", exception.getMessage());
    }

    // ==================== GET BLOCKED USERS TESTS ====================

    @Test
    void testGetBlockedUsers_Success() {
        // Arrange
        List<BlockedUser> blockedUsers = Arrays.asList(blockRelation);
        Page<BlockedUser> blockedUsersPage = new PageImpl<>(blockedUsers, PageRequest.of(0, 10), 1);

        when(blockedUserRepository.findByBlockerId(eq("blocker-123"), any(Pageable.class)))
                .thenReturn(blockedUsersPage);
        when(userRepository.findById("blocked-456")).thenReturn(Optional.of(blockedUser));

        // Act
        PaginatedResponse<BlockedUserDTO> result = blockedUserService.getBlockedUsers("blocker-123", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(0, result.getPagination().getCurrentPage());
        assertEquals(1, result.getPagination().getTotalPages());
        verify(blockedUserRepository).findByBlockerId(eq("blocker-123"), any(Pageable.class));
        verify(userRepository).findById("blocked-456");
    }

    @Test
    void testGetBlockedUsers_EmptyList() {
        // Arrange
        Page<BlockedUser> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);

        when(blockedUserRepository.findByBlockerId(eq("blocker-123"), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        PaginatedResponse<BlockedUserDTO> result = blockedUserService.getBlockedUsers("blocker-123", 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getPagination().getTotalPages());
    }

    @Test
    void testGetBlockedUsers_Pagination() {
        // Arrange
        List<BlockedUser> blockedUsers = Arrays.asList(blockRelation);
        Page<BlockedUser> blockedUsersPage = new PageImpl<>(blockedUsers, PageRequest.of(1, 5), 20);

        when(blockedUserRepository.findByBlockerId(eq("blocker-123"), any(Pageable.class)))
                .thenReturn(blockedUsersPage);
        when(userRepository.findById("blocked-456")).thenReturn(Optional.of(blockedUser));

        // Act
        PaginatedResponse<BlockedUserDTO> result = blockedUserService.getBlockedUsers("blocker-123", 1, 5);

        // Assert
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(4, result.getPagination().getTotalPages());
        assertTrue(result.getPagination().getHasNext());
        assertTrue(result.getPagination().getHasPrevious());
    }

    @Test
    void testGetBlockedUsers_MultipleBlockedUsers() {
        // Arrange
        User blockedUser2 = new User();
        blockedUser2.setUserId("blocked-789");
        blockedUser2.setUsername("blocked_user2");
        blockedUser2.setFullName("Blocked User 2");
        blockedUser2.setProfilePictureUrl("https://example.com/blocked2.jpg");

        BlockedUser blockRelation2 = new BlockedUser();
        blockRelation2.setBlockId("block-999");
        blockRelation2.setBlockerId("blocker-123");
        blockRelation2.setBlockedId("blocked-789");
        blockRelation2.setBlockedAt(LocalDateTime.now());

        List<BlockedUser> blockedUsers = Arrays.asList(blockRelation, blockRelation2);
        Page<BlockedUser> blockedUsersPage = new PageImpl<>(blockedUsers, PageRequest.of(0, 10), 2);

        when(blockedUserRepository.findByBlockerId(eq("blocker-123"), any(Pageable.class)))
                .thenReturn(blockedUsersPage);
        when(userRepository.findById("blocked-456")).thenReturn(Optional.of(blockedUser));
        when(userRepository.findById("blocked-789")).thenReturn(Optional.of(blockedUser2));

        // Act
        PaginatedResponse<BlockedUserDTO> result = blockedUserService.getBlockedUsers("blocker-123", 0, 10);

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals(2L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetBlockedUsers_WithUserDetails() {
        // Arrange
        List<BlockedUser> blockedUsers = Arrays.asList(blockRelation);
        Page<BlockedUser> blockedUsersPage = new PageImpl<>(blockedUsers, PageRequest.of(0, 10), 1);

        when(blockedUserRepository.findByBlockerId(eq("blocker-123"), any(Pageable.class)))
                .thenReturn(blockedUsersPage);
        when(userRepository.findById("blocked-456")).thenReturn(Optional.of(blockedUser));

        // Act
        PaginatedResponse<BlockedUserDTO> result = blockedUserService.getBlockedUsers("blocker-123", 0, 10);

        // Assert
        BlockedUserDTO dto = result.getData().get(0);
        assertEquals("block-789", dto.getBlockId());
        assertEquals("blocked-456", dto.getBlockedUserId());
        assertEquals("blocked_user", dto.getBlockedUsername());
        assertEquals("Blocked User", dto.getBlockedUserFullName());
        assertEquals("https://example.com/blocked.jpg", dto.getBlockedUserProfilePicture());
        assertNotNull(dto.getBlockedAt());
    }

    @Test
    void testGetBlockedUsers_UserNotFoundInRepository() {
        // Arrange
        List<BlockedUser> blockedUsers = Arrays.asList(blockRelation);
        Page<BlockedUser> blockedUsersPage = new PageImpl<>(blockedUsers, PageRequest.of(0, 10), 1);

        when(blockedUserRepository.findByBlockerId(eq("blocker-123"), any(Pageable.class)))
                .thenReturn(blockedUsersPage);
        when(userRepository.findById("blocked-456")).thenReturn(Optional.empty());

        // Act
        PaginatedResponse<BlockedUserDTO> result = blockedUserService.getBlockedUsers("blocker-123", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        BlockedUserDTO dto = result.getData().get(0);
        assertEquals("block-789", dto.getBlockId());
        assertEquals("blocked-456", dto.getBlockedUserId());
        // User details should be null since user was not found
    }

    @Test
    void testGetBlockedUsers_SortedByBlockedAtDescending() {
        // Arrange
        BlockedUser oldBlock = new BlockedUser();
        oldBlock.setBlockId("block-old");
        oldBlock.setBlockerId("blocker-123");
        oldBlock.setBlockedId("blocked-old");
        oldBlock.setBlockedAt(LocalDateTime.now().minusDays(5));

        BlockedUser newBlock = new BlockedUser();
        newBlock.setBlockId("block-new");
        newBlock.setBlockerId("blocker-123");
        newBlock.setBlockedId("blocked-new");
        newBlock.setBlockedAt(LocalDateTime.now());

        // Blocks should be sorted by blockedAt descending (newest first)
        List<BlockedUser> blockedUsers = Arrays.asList(newBlock, oldBlock);
        Page<BlockedUser> blockedUsersPage = new PageImpl<>(blockedUsers, PageRequest.of(0, 10), 2);

        when(blockedUserRepository.findByBlockerId(eq("blocker-123"), any(Pageable.class)))
                .thenReturn(blockedUsersPage);
        when(userRepository.findById("blocked-old")).thenReturn(Optional.of(blockedUser));
        when(userRepository.findById("blocked-new")).thenReturn(Optional.of(blockedUser));

        // Act
        PaginatedResponse<BlockedUserDTO> result = blockedUserService.getBlockedUsers("blocker-123", 0, 10);

        // Assert
        assertEquals(2, result.getData().size());
        // First item should be the newest block
        assertEquals("block-new", result.getData().get(0).getBlockId());
    }

    // ==================== EDGE CASES AND INTEGRATION TESTS ====================

    @Test
    void testBlockUser_ThenUnblock() {
        // Arrange - Block
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(false);
        when(blockedUserRepository.save(any(BlockedUser.class))).thenReturn(blockRelation);

        // Act - Block
        blockedUserService.blockUser("user-1", "user-2");

        // Arrange - Unblock
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(true);
        doNothing().when(blockedUserRepository).deleteByBlockerIdAndBlockedId("user-1", "user-2");

        // Act - Unblock
        blockedUserService.unblockUser("user-1", "user-2");

        // Assert
        verify(blockedUserRepository).save(any(BlockedUser.class));
        verify(blockedUserRepository).deleteByBlockerIdAndBlockedId("user-1", "user-2");
    }

    @Test
    void testIsUserBlocked_AfterBlocking() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-1", "user-2"))
                .thenReturn(false, true); // First call returns false, second returns true
        when(blockedUserRepository.save(any(BlockedUser.class))).thenReturn(blockRelation);

        // Act
        boolean beforeBlock = blockedUserService.isUserBlocked("user-1", "user-2");
        blockedUserService.blockUser("user-1", "user-2");
        boolean afterBlock = blockedUserService.isUserBlocked("user-1", "user-2");

        // Assert
        assertFalse(beforeBlock);
        assertTrue(afterBlock);
    }

    @Test
    void testGetBlockedUsers_WithCustomPageSize() {
        // Arrange
        List<BlockedUser> blockedUsers = Arrays.asList(blockRelation);
        Page<BlockedUser> blockedUsersPage = new PageImpl<>(blockedUsers, PageRequest.of(0, 20), 1);

        when(blockedUserRepository.findByBlockerId(eq("blocker-123"), any(Pageable.class)))
                .thenReturn(blockedUsersPage);
        when(userRepository.findById("blocked-456")).thenReturn(Optional.of(blockedUser));

        // Act
        PaginatedResponse<BlockedUserDTO> result = blockedUserService.getBlockedUsers("blocker-123", 0, 20);

        // Assert
        assertEquals(20, result.getPagination().getPageSize());
    }

    @Test
    void testValidateNotBlocked_ForFollowingScenario() {
        // Arrange - Simulating validation before following
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("follower-1", "followee-2"))
                .thenReturn(false);
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("followee-2", "follower-1"))
                .thenReturn(false);

        // Act & Assert - Should not throw exception
        blockedUserService.validateNotBlocked("follower-1", "followee-2");

        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("follower-1", "followee-2");
        verify(blockedUserRepository).existsByBlockerIdAndBlockedId("followee-2", "follower-1");
    }

    @Test
    void testIsBlockedInEitherDirection_WithNullUsers() {
        // Act
        boolean result = blockedUserService.isBlockedInEitherDirection(null, "user-2");

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetBlockedUsers_LargePagination() {
        // Arrange - Simulating page 10 of large result set
        List<BlockedUser> blockedUsers = Arrays.asList(blockRelation);
        Page<BlockedUser> blockedUsersPage = new PageImpl<>(blockedUsers, PageRequest.of(10, 10), 150);

        when(blockedUserRepository.findByBlockerId(eq("blocker-123"), any(Pageable.class)))
                .thenReturn(blockedUsersPage);
        when(userRepository.findById("blocked-456")).thenReturn(Optional.of(blockedUser));

        // Act
        PaginatedResponse<BlockedUserDTO> result = blockedUserService.getBlockedUsers("blocker-123", 10, 10);

        // Assert
        assertEquals(10, result.getPagination().getCurrentPage());
        assertEquals(15, result.getPagination().getTotalPages());
        assertEquals(150L, result.getPagination().getTotalItems());
        assertTrue(result.getPagination().getHasNext());
        assertTrue(result.getPagination().getHasPrevious());
    }

    @Test
    void testBlockUser_DifferentUsers() {
        // Arrange
        when(blockedUserRepository.existsByBlockerIdAndBlockedId("user-A", "user-B"))
                .thenReturn(false);
        when(blockedUserRepository.save(any(BlockedUser.class))).thenReturn(blockRelation);

        // Act
        blockedUserService.blockUser("user-A", "user-B");

        // Assert
        verify(blockedUserRepository).save(any(BlockedUser.class));
    }

    @Test
    void testGetBlockedUsers_VerifyDTOMapping() {
        // Arrange
        List<BlockedUser> blockedUsers = Arrays.asList(blockRelation);
        Page<BlockedUser> blockedUsersPage = new PageImpl<>(blockedUsers, PageRequest.of(0, 10), 1);

        when(blockedUserRepository.findByBlockerId(eq("blocker-123"), any(Pageable.class)))
                .thenReturn(blockedUsersPage);
        when(userRepository.findById("blocked-456")).thenReturn(Optional.of(blockedUser));

        // Act
        PaginatedResponse<BlockedUserDTO> result = blockedUserService.getBlockedUsers("blocker-123", 0, 10);

        // Assert - Verify all fields are mapped correctly
        BlockedUserDTO dto = result.getData().get(0);
        assertNotNull(dto.getBlockId());
        assertNotNull(dto.getBlockedUserId());
        assertNotNull(dto.getBlockedUsername());
        assertNotNull(dto.getBlockedUserFullName());
        assertNotNull(dto.getBlockedUserProfilePicture());
        assertNotNull(dto.getBlockedAt());
    }
}
