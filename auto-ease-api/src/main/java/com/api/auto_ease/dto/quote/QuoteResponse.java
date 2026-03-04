package com.api.auto_ease.dto.quote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResponse {

    private UUID id;
    private UUID jobRequestId;
    private UUID garageId;
    private String garageName;
    private String garageCity;
    private BigDecimal garageRating;
    private BigDecimal price;
    private String estimatedDuration;
    private String description;
    private String warrantyInfo;
    private String status;
}
