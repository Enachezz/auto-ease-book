package com.api.auto_ease.dto.jobrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobRequestRequest {

    @NotNull
    private Integer carId;

    private UUID categoryId;

    @NotBlank
    private String title;

    private String description;
    private String urgency;
    private LocalDate preferredDate;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String locationAddress;
    private String locationCity;
    private String locationState;
}
