package com.infy.pinterest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationSendDTO {

    @NotBlank(message = "Please provide a valid recipient username")
    private String recipientUsername;

    @NotBlank(message = "Please provide a valid board ID")
    private String boardId;

    private String message;

    @NotBlank(message = "Please provide a valid permission")
    private String permission;
}
