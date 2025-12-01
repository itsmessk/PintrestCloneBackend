package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private String userId;
    private String username;
    private String email;
    private String token;
    private Integer expiresIn;
    private String businessId; // Business profile ID if user has a business account
}

