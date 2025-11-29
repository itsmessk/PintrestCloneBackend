package com.infy.pinterest.repository;

import com.infy.pinterest.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(String userId, Boolean isRead, Pageable pageable);

    Long countByUserIdAndIsRead(String userId, Boolean isRead);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsRead(String userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.entityId = :entityId AND n.type = :type")
    void deleteByEntityIdAndType(String entityId, Notification.NotificationType type);
}
