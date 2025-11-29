package com.infy.pinterest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemographicsDTO {
    private List<AgeGroupDTO> ageGroups;
    private List<LocationDTO> topLocations;
}
