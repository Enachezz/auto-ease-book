package com.api.auto_ease.dto.service;

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
public class ServiceSearchRequest {

    @Valid
    private ServiceFilterCriteria filter;

    @Min(0)
    @Builder.Default
    private int page = 0;
}
