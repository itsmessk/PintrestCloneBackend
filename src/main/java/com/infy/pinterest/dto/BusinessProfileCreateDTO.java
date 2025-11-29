package com.infy.pinterest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfileCreateDTO {

    @NotBlank(message = "Please provide a valid business name")
    @Size(max = 100, message = "Business name must not exceed 100 characters")
    private String businessName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private String website; private String category;

    private String logoUrl;

    @Email(message = "Please provide a valid email")
    private String contactEmail;
}
