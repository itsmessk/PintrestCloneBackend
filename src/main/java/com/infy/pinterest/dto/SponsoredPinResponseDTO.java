package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SponsoredPinResponseDTO {
    private String sponsoredId;
    private String campaignName;
    private PinSummaryDTO pin;
    private String status;
    private Integer impressions;
    private Integer clicks;
    private Integer saves;
    private BigDecimal budget;
    private BigDecimal spent;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
}
