package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinSearchResultDTO {
    private String pinId;
    private String title;
    private String description;
    private String imageUrl;
    private UserSummaryDTO createdBy;
    private Integer saves;
    private Integer likes;
    private Double relevanceScore;
}
