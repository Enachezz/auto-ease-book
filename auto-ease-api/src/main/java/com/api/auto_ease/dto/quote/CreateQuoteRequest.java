package com.api.auto_ease.dto.quote;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuoteRequest {

    @NotNull
    private BigDecimal price;

    private String estimatedDuration;
    private String description;
    private String warrantyInfo;
}
