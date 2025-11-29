package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfileResponseDTO {
    private String businessId;
    private String userId;
    private String businessName;
    private String description;
    private String website;
    private String category;
    private String logoUrl;
    private String contactEmail;
    private Integer followerCount;
    private List<BoardSummaryDTO> boards;
    private LocalDateTime createdAt;
}
