package com.infy.pinterest.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private String profilePictureUrl;
    private String accountType;
    private LocalDateTime createdAt;
}
