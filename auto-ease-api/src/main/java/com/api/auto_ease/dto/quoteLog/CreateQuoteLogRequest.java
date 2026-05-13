package com.api.auto_ease.dto.quoteLog;

import com.api.auto_ease.dto.quote.CreateQuoteRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuoteLogRequest {

    private String message;
    private Boolean notificationFlag;
    private Boolean updateFlag;
    private CreateQuoteRequest newQuote;
}
