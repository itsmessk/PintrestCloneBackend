package com.infy.pinterest.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserReportDTO {

    @NotBlank(message = "Please provide a valid reason")
    private String reason;

    private String description;
}
