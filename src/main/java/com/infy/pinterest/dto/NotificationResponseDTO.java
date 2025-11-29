package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private String notificationId;
    private String type;
    private String message;
    private String entityId;
    private String entityType;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private UserSummaryDTO sender;
}
