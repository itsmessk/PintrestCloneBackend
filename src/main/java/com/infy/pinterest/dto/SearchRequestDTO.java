package com.infy.pinterest.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor

public class SearchRequestDTO {
    @NotBlank(message = "Please provide a valid search query")
    private String query;

    private String category;
    private String sortBy; // relevance, recent, popular
    private Integer page = 0;
    private Integer size = 20;

}
