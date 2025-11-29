package com.infy.pinterest.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "saved_pins")
@Data
public class SavedPin {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "save_id", length = 36)
    private String saveId;

    @Column(name = "pin_id", nullable = false, length = 36)
    private String pinId; // Original pin ID

    @Column(name = "copied_pin_id", length = 36)
    private String copiedPinId; // The new pin created in user's board

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "board_id", length = 36)
    private String boardId;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @PrePersist
    protected void onCreate() {
        savedAt = LocalDateTime.now();
    }
}
