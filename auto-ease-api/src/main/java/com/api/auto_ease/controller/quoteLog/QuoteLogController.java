package com.api.auto_ease.controller.quoteLog;

import com.api.auto_ease.dto.quoteLog.CreateQuoteLogRequest;
import com.api.auto_ease.dto.quoteLog.PagedQuoteLogsResponse;
import com.api.auto_ease.dto.quoteLog.QuoteLogResponse;
import com.api.auto_ease.service.quoteLog.QuoteLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.api.auto_ease.security.AppUserTypeSecurityExpressions.HAS_ROLE_GARAGE;

@RestController
@RequiredArgsConstructor
public class QuoteLogController {

    private final QuoteLogService quoteLogService;

    @PostMapping("/api/quotes/{quoteId}/logs")
    @PreAuthorize(HAS_ROLE_GARAGE)
    public ResponseEntity<QuoteLogResponse> createLog(Authentication auth,
                                                      @PathVariable UUID quoteId,
                                                      @RequestBody CreateQuoteLogRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quoteLogService.createLog(userId, quoteId, request));
    }

    @GetMapping("/api/quote-logs")
    public PagedQuoteLogsResponse listLogs(Authentication auth,
                                           @RequestParam(value = "notificationFlag", required = false) Boolean notificationFlag,
                                           @RequestParam(value = "page", required = false, defaultValue = "0") int page) {
        return quoteLogService.listForCurrentUser(auth, notificationFlag, page);
    }

    @PostMapping("/api/quote-logs/{logId}/mark-read")
    public QuoteLogResponse markAsRead(Authentication auth, @PathVariable UUID logId) {
        return quoteLogService.markAsRead(auth, logId);
    }
}
