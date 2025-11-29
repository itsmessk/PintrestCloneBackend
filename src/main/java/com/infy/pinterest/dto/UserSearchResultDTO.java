package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResultDTO {
    private String userId;
    private String username;
    private String fullName;
    private String profilePictureUrl;
    private String bio;
    private Integer followers;
    private Integer following;
    private Integer pinCount;
}
