package com.api.auto_ease.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptQuoteRequest {

    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String notes;
}
