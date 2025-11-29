package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO for Pin information
 * Used when displaying pins within boards or other contexts
 * where full pin details are not needed
 */
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinSummaryDTO {
    private String pinId;
    private String title;
    private String imageUrl;
}
