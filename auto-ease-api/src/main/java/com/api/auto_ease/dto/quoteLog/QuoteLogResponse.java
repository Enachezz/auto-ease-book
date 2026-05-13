package com.api.auto_ease.dto.quoteLog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteLogResponse {

    private UUID id;
    private UUID quoteId;
    private String authorUserId;
    private String message;
    private String typeOfNotification;
    private String reference;
    private Boolean notificationFlag;
    private UUID triggeredQuoteId;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
