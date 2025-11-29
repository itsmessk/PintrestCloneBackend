package com.infy.pinterest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
@Entity
@Table(name = "business_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfile {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "business_id", length = 36)
    private String businessId;


    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private String userId;

    @Column(name = "business_name", nullable = false, length = 100)
    private String businessName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "follower_count")
    private Integer followerCount = 0;

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
}
