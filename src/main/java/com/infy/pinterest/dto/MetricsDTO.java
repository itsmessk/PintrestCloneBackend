package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDTO {
    private Integer impressions;
    private Integer clicks;
    private Double clickThroughRate;
    private Integer saves;
    private Integer engagement;
    private Double engagementRate;
}
