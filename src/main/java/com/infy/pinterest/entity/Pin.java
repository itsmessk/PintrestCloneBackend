package com.infy.pinterest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "pins")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pin {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "pin_id", length = 36)
    private String pinId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "board_id", nullable = false, length = 36)
    private String boardId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Convert(converter = PinVisibilityConverter.class)
    @Column(name = "visibility")
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "is_draft")
    private Boolean isDraft = false;

    @Column(name = "is_sponsored")
    private Boolean isSponsored = false;

    @Column(name = "save_count")
    private Integer saveCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

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
