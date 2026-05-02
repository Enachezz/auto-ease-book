package com.api.auto_ease.dto.garage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GarageSearchRequest {

    @Valid
    private GarageFilterCriteria filter;

    @Min(0)
    @Builder.Default
    private int page = 0;
}
