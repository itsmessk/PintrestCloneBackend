package com.infy.pinterest.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinUpdateDTO {
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private String boardId;

    @Pattern(regexp = "^(PUBLIC|PRIVATE)$", message = "Visibility must be 'PUBLIC' or'PRIVATE'")
    private String visibility;


}
