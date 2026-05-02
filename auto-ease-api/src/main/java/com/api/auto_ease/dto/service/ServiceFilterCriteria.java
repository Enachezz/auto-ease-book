package com.api.auto_ease.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceFilterCriteria {

    /**
     * Case-insensitive partial match on service name. Null or blank applies no name constraint.
     */
    private String name;
}
