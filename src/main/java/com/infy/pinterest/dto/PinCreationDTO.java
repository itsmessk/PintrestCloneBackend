package com.infy.pinterest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinCreationDTO {
    @NotBlank(message = "Please provide a valid title")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private String sourceUrl;

    @NotBlank(message = "Please provide a valid board ID")
    private String boardId;

    @NotBlank(message = "Please provide a valid visibility")
    @Pattern(regexp = "^(PUBLIC|PRIVATE)$", message = "Visibility must be 'PUBLIC' or 'PRIVATE'")
    private String visibility;

    // Image option: URL or file upload
    // If imageUrl is provided, use it directly
    // If not provided, image file must be uploaded
    private String imageUrl;

}
