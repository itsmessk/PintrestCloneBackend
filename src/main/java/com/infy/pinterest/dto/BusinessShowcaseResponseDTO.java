package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessShowcaseResponseDTO {
    private String showcaseId;
    private String businessId;
    private String businessName;
    private String logoUrl;
    private String title;
    private String description;
    private String theme;
    private Boolean isActive;
    private List<PinSummaryDTO> pins;
    private Integer followerCount;
}
