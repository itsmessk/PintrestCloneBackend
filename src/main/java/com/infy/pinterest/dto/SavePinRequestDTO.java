package com.infy.pinterest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavePinRequestDTO {
    @NotBlank(message = "Pin ID is required")
    private String pinId;
    
    private String boardId; // Optional - which board to save to
}
