package com.infy.pinterest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SponsoredPinUpdateDTO {
    @Size(min = 2, max = 100, message = "Campaign name must be between 2 and 100 characters")
    private String campaignName;

    @DecimalMin(value = "0.01", message = "Budget must be at least 0.01")
    private BigDecimal budget;

    @Future(message = "End date must be in the future")
    private LocalDate endDate;
}
