package com.infy.pinterest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "boards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "board_id", length = 36)
    private String boardId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Convert(converter = BoardVisibilityConverter.class)
    @Column(name = "visibility")
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "is_collaborative")
    private Boolean isCollaborative = false;

    @Column(name = "pin_count")
    private Integer pinCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Visibility {
        PUBLIC, PRIVATE;
        
        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
        
        public static Visibility fromString(String value) {
            return value != null ? Visibility.valueOf(value.toUpperCase()) : null;
        }
    }

}
