package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetAudienceDTO {
    private String ageRange;
    private List<String> interests;
    private List<String> locations;
}
