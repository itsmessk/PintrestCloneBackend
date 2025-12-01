package com.infy.pinterest.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "pin_likes")
@Data
public class PinLike {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "like_id", length = 36)
    private String likeId;

    @Column(name = "pin_id", nullable = false, length = 36)
    private String pinId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "liked_at", nullable = false)
    private LocalDateTime likedAt;

    @PrePersist
    protected void onCreate() {
        likedAt = LocalDateTime.now();
    }
}
