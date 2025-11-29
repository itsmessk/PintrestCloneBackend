package com.infy.pinterest.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponseDTO {
    private String invitationId;
    private String type;
    private UserSummaryDTO from;
    private BoardSummaryDTO board;
    private String message;
    private String permission;
    private String status;
    private LocalDateTime sentAt;
}
