package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinLikeResponseDTO {
    private String likeId;
    private String pinId;
    private String userId;
    private String likedAt;
    private Boolean isLiked;
}
