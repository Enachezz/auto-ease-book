package com.api.auto_ease.dto.jobrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequestResponse {

    private UUID id;
    private Integer carId;
    private String makeName;
    private String modelName;
    private Integer carYear;
    private UUID categoryId;
    private String categoryName;
    private String title;
    private String description;
    private String urgency;
    private LocalDate preferredDate;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String status;
    private String locationAddress;
    private String locationCity;
    private String locationState;
    private Integer quoteCount;
}
