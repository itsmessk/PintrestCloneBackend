package com.infy.pinterest.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BusinessShowcaseUpdateDTO {
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 50, message = "Theme cannot exceed 50 characters")
    private String theme;

    private Boolean isActive;
}
