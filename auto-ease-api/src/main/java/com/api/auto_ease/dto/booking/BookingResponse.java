package com.api.auto_ease.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private UUID id;
    private UUID quoteId;
    private UUID garageId;
    private String garageName;
    private String jobTitle;
    private BigDecimal price;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String status;
    private String notes;
}
