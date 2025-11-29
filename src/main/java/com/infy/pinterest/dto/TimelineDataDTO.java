package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimelineDataDTO {
    private LocalDate date;
    private Integer impressions;
    private Integer clicks;
    private Integer saves;
}
