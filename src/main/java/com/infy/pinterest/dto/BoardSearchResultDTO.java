package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardSearchResultDTO {
    private String boardId;
    private String name;
    private String description;
    private String coverImageUrl;
    private UserSummaryDTO createdBy;
    private Integer pinCount;
    private Integer followers;
}
