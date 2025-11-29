package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignAnalyticsDTO {
    private String campaignId;
    private String campaignName;
    private MetricsDTO metrics;
    private DemographicsDTO demographics;
    private SpendingDTO spending;
    private List<TimelineDataDTO> timeline;
}
