package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class PaginationDTO {
    private Integer currentPage;
    private Integer totalPages;
    private Long totalItems;
    private Integer pageSize;
    private Boolean hasNext;
    private Boolean hasPrevious;

}
