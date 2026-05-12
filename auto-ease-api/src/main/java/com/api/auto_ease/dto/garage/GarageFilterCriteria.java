package com.api.auto_ease.dto.garage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GarageFilterCriteria {

    /**
     * Case-insensitive partial match on {@link com.api.auto_ease.domain.garage.Garage#getBusinessName()}.
     */
    private String businessName;

    /**
     * When non-empty, only garages linked to at least one of these {@link com.api.auto_ease.domain.serviceCategory.ServiceCategory} ids
     * (via {@link com.api.auto_ease.domain.garageCategory.GarageCategoryAssignment}). Null or empty applies no category filter.
     */
    private Set<UUID> categoryIds;

    /**
     * When non-null, only garages whose {@link com.api.auto_ease.domain.garage.Garage#isDealership()} matches.
     * Null applies no dealership filter.
     */
    private Boolean isDealership;
}
