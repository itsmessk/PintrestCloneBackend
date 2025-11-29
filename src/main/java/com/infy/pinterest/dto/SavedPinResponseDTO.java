package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedPinResponseDTO {
    private String saveId;
    private String pinId;
    private String userId;
    private String boardId;
    private String savedAt;
    private Boolean isSaved;
}
