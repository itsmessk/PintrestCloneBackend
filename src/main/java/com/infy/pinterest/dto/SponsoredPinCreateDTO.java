package com.infy.pinterest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SponsoredPinCreateDTO {

    @NotBlank(message = "Please provide a valid pin ID")
    private String pinId;

    @NotBlank(message = "Please provide a valid campaign name")
    private String campaignName;

    @NotNull(message = "Please provide a valid budget")
    @Positive(message = "Budget must be positive")
    private BigDecimal budget;

    @NotNull(message = "Please provide a valid start date")
    private LocalDate startDate;

    @NotNull(message = "Please provide a valid end date")
    private LocalDate endDate;

    private TargetAudienceDTO targetAudience;
}
