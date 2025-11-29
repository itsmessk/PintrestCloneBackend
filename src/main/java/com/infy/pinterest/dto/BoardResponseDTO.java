package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponseDTO {
    private String boardId;
    private String userId;
    private String name;
    private String description;
    private String category;
    private String coverImageUrl;
    private String visibility;
    private Boolean isCollaborative;
    private Integer pinCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
