package com.infy.pinterest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.infy.pinterest.dto.NotificationResponseDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.entity.Notification;
import com.infy.pinterest.entity.Notification.NotificationType;
import com.infy.pinterest.entity.User;
import com.infy.pinterest.repository.NotificationRepository;
import com.infy.pinterest.repository.UserRepository;
import com.infy.pinterest.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User sender;
    private User recipient;
    private Notification notification;

    @BeforeEach
    void setUp() {
        // Setup sender user
        sender = new User();
        sender.setUserId("sender-123");
        sender.setUsername("sender_user");
        sender.setEmail("sender@example.com");
        sender.setFullName("Sender User");
        sender.setProfilePictureUrl("https://example.com/sender.jpg");
        sender.setIsActive(true);
        sender.setCreatedAt(LocalDateTime.now());

        // Setup recipient user
        recipient = new User();
        recipient.setUserId("recipient-456");
        recipient.setUsername("recipient_user");
        recipient.setEmail("recipient@example.com");
        recipient.setFullName("Recipient User");
        recipient.setProfilePictureUrl("https://example.com/recipient.jpg");
        recipient.setIsActive(true);
        recipient.setCreatedAt(LocalDateTime.now());

        // Setup notification
        notification = new Notification();
        notification.setNotificationId("notification-789");
        notification.setUserId("recipient-456");
        notification.setSenderId("sender-123");
        notification.setType(NotificationType.NEW_FOLLOWER);
        notification.setMessage("sender_user started following you");
        notification.setEntityId("follow-001");
        notification.setEntityType("FOLLOW");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
    }

    // ==================== CREATE NOTIFICATION TESTS ====================

    @Test
    void testCreateNotification_Success() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        notificationService.createNotification(
            "recipient-456", 
            "sender-123", 
            NotificationType.NEW_FOLLOWER, 
            "sender_user started following you",
            "follow-001",
            "FOLLOW"
        );

        // Assert
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateNotification_InvitationReceived() {
        // Arrange
        Notification inviteNotification = new Notification();
        inviteNotification.setType(NotificationType.INVITATION_RECEIVED);
        inviteNotification.setMessage("You have been invited to collaborate");
        when(notificationRepository.save(any(Notification.class))).thenReturn(inviteNotification);

        // Act
        notificationService.createNotification(
            "user-1", 
            "user-2", 
            NotificationType.INVITATION_RECEIVED, 
            "You have been invited to collaborate",
            "invitation-123",
            "INVITATION"
        );

        // Assert
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateNotification_PinLiked() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        notificationService.createNotification(
            "pin-owner-123", 
            "liker-456", 
            NotificationType.PIN_LIKED, 
            "liker_user liked your pin",
            "pin-789",
            "PIN"
        );

        // Assert
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateNotification_PinSaved() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        notificationService.createNotification(
            "pin-owner-123", 
            "saver-456", 
            NotificationType.PIN_SAVED, 
            "saver_user saved your pin",
            "pin-789",
            "PIN"
        );

        // Assert
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateNotification_BoardShared() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        notificationService.createNotification(
            "user-1", 
            "sharer-2", 
            NotificationType.BOARD_SHARED, 
            "Board was shared with you",
            "board-123",
            "BOARD"
        );

        // Assert
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateNotification_WithNullSender() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        notificationService.createNotification(
            "recipient-456", 
            null, 
            NotificationType.COLLABORATION_ADDED, 
            "You were added as a collaborator",
            "board-123",
            "BOARD"
        );

        // Assert
        verify(notificationRepository).save(any(Notification.class));
    }

    // ==================== GET NOTIFICATIONS TESTS ====================

    @Test
    void testGetNotifications_Success_AllNotifications() {
        // Arrange
        List<Notification> notifications = Arrays.asList(notification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(0, 10), 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(userRepository.findById("sender-123")).thenReturn(Optional.of(sender));

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(0, result.getPagination().getCurrentPage());
        assertEquals(1, result.getPagination().getTotalPages());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class));
    }

    @Test
    void testGetNotifications_FilterByUnread() {
        // Arrange
        List<Notification> unreadNotifications = Arrays.asList(notification);
        Page<Notification> notificationPage = new PageImpl<>(unreadNotifications, PageRequest.of(0, 10), 1);

        when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(eq("recipient-456"), eq(false), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(userRepository.findById("sender-123")).thenReturn(Optional.of(sender));

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", false, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertFalse(result.getData().get(0).getIsRead());
        verify(notificationRepository).findByUserIdAndIsReadOrderByCreatedAtDesc(eq("recipient-456"), eq(false), any(Pageable.class));
    }

    @Test
    void testGetNotifications_FilterByRead() {
        // Arrange
        Notification readNotification = new Notification();
        readNotification.setNotificationId("read-notification-999");
        readNotification.setUserId("recipient-456");
        readNotification.setSenderId("sender-123");
        readNotification.setType(NotificationType.PIN_LIKED);
        readNotification.setMessage("Someone liked your pin");
        readNotification.setIsRead(true);
        readNotification.setReadAt(LocalDateTime.now());
        readNotification.setCreatedAt(LocalDateTime.now());

        List<Notification> readNotifications = Arrays.asList(readNotification);
        Page<Notification> notificationPage = new PageImpl<>(readNotifications, PageRequest.of(0, 10), 1);

        when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(eq("recipient-456"), eq(true), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(userRepository.findById("sender-123")).thenReturn(Optional.of(sender));

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", true, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertTrue(result.getData().get(0).getIsRead());
        assertNotNull(result.getData().get(0).getReadAt());
    }

    @Test
    void testGetNotifications_EmptyList() {
        // Arrange
        Page<Notification> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getPagination().getTotalPages());
    }

    @Test
    void testGetNotifications_WithPagination() {
        // Arrange
        List<Notification> notifications = Arrays.asList(notification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(1, 5), 20);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(userRepository.findById("sender-123")).thenReturn(Optional.of(sender));

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 1, 5);

        // Assert
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(4, result.getPagination().getTotalPages());
        assertEquals(20L, result.getPagination().getTotalItems());
        assertTrue(result.getPagination().getHasNext());
        assertTrue(result.getPagination().getHasPrevious());
    }

    @Test
    void testGetNotifications_MultipleNotifications() {
        // Arrange
        Notification notification2 = new Notification();
        notification2.setNotificationId("notification-888");
        notification2.setUserId("recipient-456");
        notification2.setSenderId("sender-123");
        notification2.setType(NotificationType.PIN_SAVED);
        notification2.setMessage("Someone saved your pin");
        notification2.setIsRead(false);
        notification2.setCreatedAt(LocalDateTime.now());

        List<Notification> notifications = Arrays.asList(notification, notification2);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(0, 10), 2);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(userRepository.findById("sender-123")).thenReturn(Optional.of(sender));

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 10);

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals(2L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetNotifications_WithSenderDetails() {
        // Arrange
        List<Notification> notifications = Arrays.asList(notification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(0, 10), 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(userRepository.findById("sender-123")).thenReturn(Optional.of(sender));

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 10);

        // Assert
        NotificationResponseDTO dto = result.getData().get(0);
        assertNotNull(dto.getSender());
        assertEquals("sender-123", dto.getSender().getUserId());
        assertEquals("sender_user", dto.getSender().getUsername());
        assertEquals("https://example.com/sender.jpg", dto.getSender().getProfilePictureUrl());
    }

    @Test
    void testGetNotifications_SenderNotFound() {
        // Arrange
        List<Notification> notifications = Arrays.asList(notification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(0, 10), 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(userRepository.findById("sender-123")).thenReturn(Optional.empty());

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertNull(result.getData().get(0).getSender());
    }

    @Test
    void testGetNotifications_NotificationWithoutSender() {
        // Arrange
        Notification noSenderNotification = new Notification();
        noSenderNotification.setNotificationId("no-sender-999");
        noSenderNotification.setUserId("recipient-456");
        noSenderNotification.setSenderId(null);
        noSenderNotification.setType(NotificationType.COLLABORATION_ADDED);
        noSenderNotification.setMessage("System notification");
        noSenderNotification.setIsRead(false);
        noSenderNotification.setCreatedAt(LocalDateTime.now());

        List<Notification> notifications = Arrays.asList(noSenderNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(0, 10), 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertNull(result.getData().get(0).getSender());
        verify(userRepository, never()).findById(anyString());
    }

    // ==================== GET UNREAD COUNT TESTS ====================

    @Test
    void testGetUnreadCount_Success() {
        // Arrange
        when(notificationRepository.countByUserIdAndIsRead("recipient-456", false))
                .thenReturn(5L);

        // Act
        Long count = notificationService.getUnreadCount("recipient-456");

        // Assert
        assertEquals(5L, count);
        verify(notificationRepository).countByUserIdAndIsRead("recipient-456", false);
    }

    @Test
    void testGetUnreadCount_NoUnreadNotifications() {
        // Arrange
        when(notificationRepository.countByUserIdAndIsRead("recipient-456", false))
                .thenReturn(0L);

        // Act
        Long count = notificationService.getUnreadCount("recipient-456");

        // Assert
        assertEquals(0L, count);
    }

    @Test
    void testGetUnreadCount_LargeCount() {
        // Arrange
        when(notificationRepository.countByUserIdAndIsRead("recipient-456", false))
                .thenReturn(1000L);

        // Act
        Long count = notificationService.getUnreadCount("recipient-456");

        // Assert
        assertEquals(1000L, count);
    }

    // ==================== MARK AS READ TESTS ====================

    @Test
    void testMarkAsRead_Success() {
        // Arrange
        when(notificationRepository.findById("notification-789")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        notificationService.markAsRead("notification-789", "recipient-456");

        // Assert
        verify(notificationRepository).findById("notification-789");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testMarkAsRead_NotificationNotFound() {
        // Arrange
        when(notificationRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationService.markAsRead("non-existent", "recipient-456");
        });

        assertEquals("Notification not found", exception.getMessage());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testMarkAsRead_UnauthorizedAccess() {
        // Arrange
        when(notificationRepository.findById("notification-789")).thenReturn(Optional.of(notification));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationService.markAsRead("notification-789", "wrong-user-999");
        });

        assertEquals("Unauthorized access to notification", exception.getMessage());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testMarkAsRead_UpdatesReadStatus() {
        // Arrange
        Notification unreadNotification = new Notification();
        unreadNotification.setNotificationId("notification-789");
        unreadNotification.setUserId("recipient-456");
        unreadNotification.setIsRead(false);
        unreadNotification.setReadAt(null);

        when(notificationRepository.findById("notification-789")).thenReturn(Optional.of(unreadNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(unreadNotification);

        // Act
        notificationService.markAsRead("notification-789", "recipient-456");

        // Assert
        assertTrue(unreadNotification.getIsRead());
        assertNotNull(unreadNotification.getReadAt());
        verify(notificationRepository).save(unreadNotification);
    }

    // ==================== MARK ALL AS READ TESTS ====================

    @Test
    void testMarkAllAsRead_Success() {
        // Arrange
        doNothing().when(notificationRepository).markAllAsRead("recipient-456");

        // Act
        notificationService.markAllAsRead("recipient-456");

        // Assert
        verify(notificationRepository).markAllAsRead("recipient-456");
    }

    @Test
    void testMarkAllAsRead_NoUnreadNotifications() {
        // Arrange
        doNothing().when(notificationRepository).markAllAsRead("user-with-no-unread");

        // Act
        notificationService.markAllAsRead("user-with-no-unread");

        // Assert
        verify(notificationRepository).markAllAsRead("user-with-no-unread");
    }

    @Test
    void testMarkAllAsRead_MultipleUsers() {
        // Arrange
        doNothing().when(notificationRepository).markAllAsRead("user-1");
        doNothing().when(notificationRepository).markAllAsRead("user-2");

        // Act
        notificationService.markAllAsRead("user-1");
        notificationService.markAllAsRead("user-2");

        // Assert
        verify(notificationRepository).markAllAsRead("user-1");
        verify(notificationRepository).markAllAsRead("user-2");
    }

    // ==================== DELETE NOTIFICATION TESTS ====================

    @Test
    void testDeleteNotification_Success() {
        // Arrange
        when(notificationRepository.findById("notification-789")).thenReturn(Optional.of(notification));
        doNothing().when(notificationRepository).delete(notification);

        // Act
        notificationService.deleteNotification("notification-789", "recipient-456");

        // Assert
        verify(notificationRepository).findById("notification-789");
        verify(notificationRepository).delete(notification);
    }

    @Test
    void testDeleteNotification_NotificationNotFound() {
        // Arrange
        when(notificationRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationService.deleteNotification("non-existent", "recipient-456");
        });

        assertEquals("Notification not found", exception.getMessage());
        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void testDeleteNotification_UnauthorizedAccess() {
        // Arrange
        when(notificationRepository.findById("notification-789")).thenReturn(Optional.of(notification));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationService.deleteNotification("notification-789", "wrong-user-999");
        });

        assertEquals("Unauthorized access to notification", exception.getMessage());
        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void testDeleteNotification_RemovesNotification() {
        // Arrange
        when(notificationRepository.findById("notification-789")).thenReturn(Optional.of(notification));
        doNothing().when(notificationRepository).delete(notification);

        // Act
        notificationService.deleteNotification("notification-789", "recipient-456");

        // Assert
        verify(notificationRepository).delete(notification);
    }

    // ==================== DELETE NOTIFICATIONS BY ENTITY TESTS ====================

    @Test
    void testDeleteNotificationsByEntity_Success() {
        // Arrange
        doNothing().when(notificationRepository).deleteByEntityIdAndType("invitation-123", NotificationType.INVITATION_RECEIVED);

        // Act
        notificationService.deleteNotificationsByEntity("invitation-123", NotificationType.INVITATION_RECEIVED);

        // Assert
        verify(notificationRepository).deleteByEntityIdAndType("invitation-123", NotificationType.INVITATION_RECEIVED);
    }

    @Test
    void testDeleteNotificationsByEntity_InvitationDeclined() {
        // Arrange
        doNothing().when(notificationRepository).deleteByEntityIdAndType("invitation-456", NotificationType.INVITATION_RECEIVED);

        // Act
        notificationService.deleteNotificationsByEntity("invitation-456", NotificationType.INVITATION_RECEIVED);

        // Assert
        verify(notificationRepository).deleteByEntityIdAndType("invitation-456", NotificationType.INVITATION_RECEIVED);
    }

    @Test
    void testDeleteNotificationsByEntity_PinDeleted() {
        // Arrange
        doNothing().when(notificationRepository).deleteByEntityIdAndType("pin-789", NotificationType.PIN_LIKED);

        // Act
        notificationService.deleteNotificationsByEntity("pin-789", NotificationType.PIN_LIKED);

        // Assert
        verify(notificationRepository).deleteByEntityIdAndType("pin-789", NotificationType.PIN_LIKED);
    }

    @Test
    void testDeleteNotificationsByEntity_BoardDeleted() {
        // Arrange
        doNothing().when(notificationRepository).deleteByEntityIdAndType("board-123", NotificationType.BOARD_SHARED);

        // Act
        notificationService.deleteNotificationsByEntity("board-123", NotificationType.BOARD_SHARED);

        // Assert
        verify(notificationRepository).deleteByEntityIdAndType("board-123", NotificationType.BOARD_SHARED);
    }

    // ==================== NOTIFICATION TYPES TESTS ====================

    @Test
    void testCreateNotification_AllNotificationTypes() {
        // Arrange
        NotificationType[] allTypes = NotificationType.values();
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act & Assert
        for (NotificationType type : allTypes) {
            notificationService.createNotification(
                "recipient-456",
                "sender-123",
                type,
                "Test message for " + type.name(),
                "entity-123",
                "TEST_ENTITY"
            );
        }

        // Verify save was called for each type
        verify(notificationRepository, org.mockito.Mockito.times(allTypes.length)).save(any(Notification.class));
    }

    @Test
    void testGetNotifications_DifferentNotificationTypes() {
        // Arrange
        Notification invitationNotification = new Notification();
        invitationNotification.setNotificationId("invite-001");
        invitationNotification.setUserId("recipient-456");
        invitationNotification.setType(NotificationType.INVITATION_RECEIVED);
        invitationNotification.setMessage("You have been invited");
        invitationNotification.setIsRead(false);
        invitationNotification.setCreatedAt(LocalDateTime.now());

        Notification followerNotification = new Notification();
        followerNotification.setNotificationId("follow-001");
        followerNotification.setUserId("recipient-456");
        followerNotification.setType(NotificationType.NEW_FOLLOWER);
        followerNotification.setMessage("New follower");
        followerNotification.setIsRead(false);
        followerNotification.setCreatedAt(LocalDateTime.now());

        List<Notification> notifications = Arrays.asList(invitationNotification, followerNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(0, 10), 2);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 10);

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals("INVITATION_RECEIVED", result.getData().get(0).getType());
        assertEquals("NEW_FOLLOWER", result.getData().get(1).getType());
    }

    // ==================== EDGE CASES AND INTEGRATION TESTS ====================

    @Test
    void testGetNotifications_OrderedByCreatedAtDesc() {
        // Arrange
        Notification oldNotification = new Notification();
        oldNotification.setNotificationId("old-001");
        oldNotification.setUserId("recipient-456");
        oldNotification.setType(NotificationType.PIN_LIKED);
        oldNotification.setMessage("Old notification");
        oldNotification.setIsRead(false);
        oldNotification.setCreatedAt(LocalDateTime.now().minusDays(5));

        Notification newNotification = new Notification();
        newNotification.setNotificationId("new-001");
        newNotification.setUserId("recipient-456");
        newNotification.setType(NotificationType.NEW_FOLLOWER);
        newNotification.setMessage("New notification");
        newNotification.setIsRead(false);
        newNotification.setCreatedAt(LocalDateTime.now());

        // Notifications should be ordered by createdAt descending
        List<Notification> notifications = Arrays.asList(newNotification, oldNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(0, 10), 2);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 10);

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals("new-001", result.getData().get(0).getNotificationId());
        assertEquals("old-001", result.getData().get(1).getNotificationId());
    }

    @Test
    void testCreateThenMarkAsRead() {
        // Arrange
        Notification savedNotification = new Notification();
        savedNotification.setNotificationId("new-notification-123");
        savedNotification.setUserId("recipient-456");
        savedNotification.setIsRead(false);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationRepository.findById("new-notification-123")).thenReturn(Optional.of(savedNotification));

        // Act - Create notification
        notificationService.createNotification(
            "recipient-456",
            "sender-123",
            NotificationType.PIN_LIKED,
            "Someone liked your pin",
            "pin-789",
            "PIN"
        );

        // Act - Mark as read
        notificationService.markAsRead("new-notification-123", "recipient-456");

        // Assert
        assertTrue(savedNotification.getIsRead());
        assertNotNull(savedNotification.getReadAt());
    }

    @Test
    void testGetNotifications_LargePagination() {
        // Arrange
        List<Notification> notifications = Arrays.asList(notification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(10, 10), 150);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(userRepository.findById("sender-123")).thenReturn(Optional.of(sender));

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 10, 10);

        // Assert
        assertEquals(10, result.getPagination().getCurrentPage());
        assertEquals(15, result.getPagination().getTotalPages());
        assertEquals(150L, result.getPagination().getTotalItems());
        assertTrue(result.getPagination().getHasNext());
        assertTrue(result.getPagination().getHasPrevious());
    }

    @Test
    void testGetNotifications_CustomPageSize() {
        // Arrange
        List<Notification> notifications = Arrays.asList(notification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(0, 20), 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(userRepository.findById("sender-123")).thenReturn(Optional.of(sender));

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 20);

        // Assert
        assertEquals(20, result.getPagination().getPageSize());
    }

    @Test
    void testGetNotifications_VerifyDTOMapping() {
        // Arrange
        List<Notification> notifications = Arrays.asList(notification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(0, 10), 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(userRepository.findById("sender-123")).thenReturn(Optional.of(sender));

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 10);

        // Assert
        NotificationResponseDTO dto = result.getData().get(0);
        assertEquals("notification-789", dto.getNotificationId());
        assertEquals("NEW_FOLLOWER", dto.getType());
        assertEquals("sender_user started following you", dto.getMessage());
        assertEquals("follow-001", dto.getEntityId());
        assertEquals("FOLLOW", dto.getEntityType());
        assertFalse(dto.getIsRead());
        assertNotNull(dto.getCreatedAt());
        assertNull(dto.getReadAt());
        assertNotNull(dto.getSender());
    }

    @Test
    void testMarkAllAsRead_ThenGetUnreadCount() {
        // Arrange
        doNothing().when(notificationRepository).markAllAsRead("recipient-456");
        when(notificationRepository.countByUserIdAndIsRead("recipient-456", false))
                .thenReturn(5L, 0L);

        // Act
        Long beforeCount = notificationService.getUnreadCount("recipient-456");
        notificationService.markAllAsRead("recipient-456");
        Long afterCount = notificationService.getUnreadCount("recipient-456");

        // Assert
        assertEquals(5L, beforeCount);
        assertEquals(0L, afterCount);
    }

    @Test
    void testDeleteNotification_ThenGetNotifications() {
        // Arrange
        when(notificationRepository.findById("notification-789")).thenReturn(Optional.of(notification));
        doNothing().when(notificationRepository).delete(notification);

        Page<Notification> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        notificationService.deleteNotification("notification-789", "recipient-456");
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 10);

        // Assert
        verify(notificationRepository).delete(notification);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testGetNotifications_FirstPage() {
        // Arrange
        List<Notification> notifications = Arrays.asList(notification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(0, 10), 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("recipient-456"), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(userRepository.findById("sender-123")).thenReturn(Optional.of(sender));

        // Act
        PaginatedResponse<NotificationResponseDTO> result = notificationService.getNotifications("recipient-456", null, 0, 10);

        // Assert
        assertEquals(0, result.getPagination().getCurrentPage());
        assertFalse(result.getPagination().getHasNext());
        assertFalse(result.getPagination().getHasPrevious());
    }

    @Test
    void testCreateNotification_VerifyAllFieldsSet() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            assertEquals("recipient-456", saved.getUserId());
            assertEquals("sender-123", saved.getSenderId());
            assertEquals(NotificationType.BOARD_SHARED, saved.getType());
            assertEquals("Board shared message", saved.getMessage());
            assertEquals("board-999", saved.getEntityId());
            assertEquals("BOARD", saved.getEntityType());
            return saved;
        });

        // Act
        notificationService.createNotification(
            "recipient-456",
            "sender-123",
            NotificationType.BOARD_SHARED,
            "Board shared message",
            "board-999",
            "BOARD"
        );

        // Assert
        verify(notificationRepository).save(any(Notification.class));
    }
}
