package com.infy.pinterest.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponseActionDTO {

    @NotBlank(message = "Please provide a valid action")
    @Pattern(regexp = "^(accept|decline|ignore)$",
            message = "Action must be 'accept', 'decline', or 'ignore'")
    private String action;
}
