package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpendingDTO {
    private BigDecimal budget;
    private BigDecimal spent;
    private BigDecimal remaining;
    private BigDecimal costPerClick;
    private BigDecimal costPerSave;
}
