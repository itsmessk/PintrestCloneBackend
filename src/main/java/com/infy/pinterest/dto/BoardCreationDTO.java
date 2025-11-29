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
public class BoardCreationDTO {
    @NotBlank(message = "Please provide a valid name")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    private String category;

    @NotBlank(message = "Please provide a valid visibility")
    @Pattern(regexp = "^(PUBLIC|PRIVATE)$", message = "Visibility must be 'PUBLIC' or 'PRIVATE'")
    private String visibility;

    // Banner image option: URL or file upload
    // If bannerImageUrl is provided, use it directly
    // If not provided, banner image file can be uploaded (optional)
    private String bannerImageUrl;

}
