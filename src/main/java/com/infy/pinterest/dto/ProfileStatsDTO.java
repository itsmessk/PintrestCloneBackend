package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileStatsDTO {
    private Long totalPins;
    private Long totalBoards;
    private Long followers;
    private Long following;
    private Long totalPinSaves;
    private Long totalPinLikes;
}
