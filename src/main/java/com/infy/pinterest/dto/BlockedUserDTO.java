package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUserDTO {
    private String blockId;
    private String blockedUserId;
    private String blockedUsername;
    private String blockedUserFullName;
    private String blockedUserProfilePicture;
    private LocalDateTime blockedAt;
}
