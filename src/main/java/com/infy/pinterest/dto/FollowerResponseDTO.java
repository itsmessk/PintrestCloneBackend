package com.infy.pinterest.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowerResponseDTO {
    private String userId;
    private String username;
    private String fullName;
    private String profilePictureUrl;
    private String bio;
    private Boolean isFollowing;
    private LocalDateTime followedAt;
}
