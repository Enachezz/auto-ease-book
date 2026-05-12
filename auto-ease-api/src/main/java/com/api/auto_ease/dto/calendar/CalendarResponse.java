package com.api.auto_ease.dto.calendar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarResponse {

    private UUID id;
    private UUID garageId;
    private String customerId;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String status;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
