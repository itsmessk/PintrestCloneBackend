package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private String profilePictureUrl;
    private String mobileNumber;
    private String accountType;
    private Boolean isActive;
    private LocalDateTime createdAt;

    // Statistics
    private ProfileStatsDTO stats;

    // Social info (if viewing another user's profile)
    private Boolean isFollowing;
    private Boolean isFollower;
    private Boolean isBlocked;
}

