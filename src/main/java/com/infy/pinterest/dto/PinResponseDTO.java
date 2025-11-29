package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinResponseDTO {
    private String pinId;
    private String userId;
    private String boardId;
    private String title;
    private String description;
    private String imageUrl;
    private String sourceUrl;
    private String visibility;
    private Boolean isDraft;
    private Boolean isSponsored;
    private Integer saveCount;
    private Integer likeCount;
    private Boolean isLiked;
    private Boolean isSaved;
    private UserSummaryDTO createdBy;
    private BoardSummaryDTO board;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
