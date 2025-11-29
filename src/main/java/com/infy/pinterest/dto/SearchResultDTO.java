package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO<T>{

    private String query;
    private List<T> results;
    private List<String> suggestions;
    private PaginationDTO pagination;
    private Long totalResults;
}
