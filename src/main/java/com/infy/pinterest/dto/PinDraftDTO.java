package com.infy.pinterest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinDraftDTO {
    @NotBlank(message = "Please provide a valid title")
    @Size(max = 200, message = "Title must not exceed 200 characters")

    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Please provide a valid board ID")
    private String boardId;
}
