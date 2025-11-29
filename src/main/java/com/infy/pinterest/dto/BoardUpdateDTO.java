package com.infy.pinterest.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardUpdateDTO {
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @Pattern(regexp = "^(PUBLIC|PRIVATE)$", message = "Visibility must be 'PUBLIC' or'PRIVATE'")
    private String visibility;

}
