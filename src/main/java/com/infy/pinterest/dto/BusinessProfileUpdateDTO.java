package com.infy.pinterest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BusinessProfileUpdateDTO {
    @Size(min = 2, max = 100, message = "Business name must be between 2 and 100 characters")
    private String businessName;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private String website;

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;

    private String logoUrl;

    @Email(message = "Contact email must be a valid email address")
    private String contactEmail;
}
