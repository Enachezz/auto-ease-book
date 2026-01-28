package com.api.auto_ease.service.serviceEntry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientServiceEntryRequest {

    private String clientUuid;
    private String serviceUuid;
    private String entryType;
    private String carMake;
    private String carModel;
    private Integer carYear;
    private String carVin;
    private String description;
    private LocalDate serviceDate;
    private String location;
    private Integer priority;
    private BigDecimal cost;
    private String note;
}


