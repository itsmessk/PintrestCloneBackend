package com.infy.pinterest.service;

import com.infy.pinterest.dto.NotificationResponseDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PaginationDTO;
import com.infy.pinterest.dto.UserSummaryDTO;
import com.infy.pinterest.entity.Notification;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.exception.UnauthorizedAccessException;
import com.infy.pinterest.repository.NotificationRepository;
import com.infy.pinterest.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a notification
     */
    @Transactional
    public void createNotification(String userId, String senderId, Notification.NotificationType type, 
                                   String message, String entityId, String entityType) {
        log.info("Creating notification for user {} of type {}", userId, type);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setEntityId(entityId);
        notification.setEntityType(entityType);

        notificationRepository.save(notification);
    }

    /**
     * Get user's notifications
     */
    public PaginatedResponse<NotificationResponseDTO> getNotifications(String userId, Boolean isRead, int page, int size) {
        log.info("Getting notifications for user {}, isRead: {}", userId, isRead);

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage;

        if (isRead != null) {
            notificationPage = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable);
        } else {
            notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        List<NotificationResponseDTO> notifications = notificationPage.getContent().stream()
                .map(this::buildNotificationResponse)
                .toList();

        PaginationDTO pagination = new PaginationDTO(
                notificationPage.getNumber(),
                notificationPage.getTotalPages(),
                notificationPage.getTotalElements(),
                notificationPage.getSize(),
                notificationPage.hasNext(),
                notificationPage.hasPrevious()
        );

        return new PaginatedResponse<>(notifications, pagination);
    }

    /**
     * Get unread notification count
     */
    public Long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(String notificationId, String userId) {
        log.info("Marking notification {} as read", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Unauthorized access to notification");
        }

        notification.setIsRead(true);
        notification.setReadAt(java.time.LocalDateTime.now());
        notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read
     */
    @Transactional
    public void markAllAsRead(String userId) {
        log.info("Marking all notifications as read for user {}", userId);
        notificationRepository.markAllAsRead(userId);
    }

    /**
     * Delete notification
     */
    @Transactional
    public void deleteNotification(String notificationId, String userId) {
        log.info("Deleting notification {}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Unauthorized access to notification");
        }

        notificationRepository.delete(notification);
    }

    /**
     * Delete notifications by entity (used when invitation is declined/ignored)
     */
    @Transactional
    public void deleteNotificationsByEntity(String entityId, Notification.NotificationType type) {
        log.info("Deleting notifications for entity {} of type {}", entityId, type);
        notificationRepository.deleteByEntityIdAndType(entityId, type);
    }

    /**
     * Build notification response
     */
    private NotificationResponseDTO buildNotificationResponse(Notification notification) {
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setNotificationId(notification.getNotificationId());
        dto.setType(notification.getType().name());
        dto.setMessage(notification.getMessage());
        dto.setEntityId(notification.getEntityId());
        dto.setEntityType(notification.getEntityType());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReadAt(notification.getReadAt());

        if (notification.getSenderId() != null) {
            userRepository.findById(notification.getSenderId()).ifPresent(user -> {
                UserSummaryDTO userSummary = new UserSummaryDTO();
                userSummary.setUserId(user.getUserId());
                userSummary.setUsername(user.getUsername());
                userSummary.setProfilePictureUrl(user.getProfilePictureUrl());
                dto.setSender(userSummary);
            });
        }

        return dto;
    }
}
