package com.infy.pinterest.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatsDTO {
    private Integer followers;
    private Integer following;
}
