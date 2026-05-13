package com.api.auto_ease.dto.quoteLog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedQuoteLogsResponse {

    private List<QuoteLogResponse> logs;
    private long totalCount;
    private int page;
    private int pageSize;
    private int totalPages;
}
