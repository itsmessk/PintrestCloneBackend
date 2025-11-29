package com.infy.pinterest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "notification_id", length = 36)
    private String notificationId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "sender_id", length = 36)
    private String senderId;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        INVITATION_RECEIVED,
        INVITATION_ACCEPTED,
        INVITATION_DECLINED,
        NEW_FOLLOWER,
        PIN_SAVED,
        PIN_LIKED,
        BOARD_SHARED,
        COLLABORATION_ADDED
    }
}
