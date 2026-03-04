package com.api.auto_ease.controller.quote;

import com.api.auto_ease.dto.quote.CreateQuoteRequest;
import com.api.auto_ease.dto.quote.QuoteResponse;
import com.api.auto_ease.service.quote.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping("/api/job-requests/{jobRequestId}/quotes")
    @PreAuthorize("hasRole('GARAGE')")
    public ResponseEntity<QuoteResponse> submitQuote(Authentication auth,
                                                      @PathVariable UUID jobRequestId,
                                                      @Valid @RequestBody CreateQuoteRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.submitQuote(userId, jobRequestId, request));
    }

    @GetMapping("/api/job-requests/{jobRequestId}/quotes")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public ResponseEntity<List<QuoteResponse>> getQuotesForRequest(Authentication auth,
                                                                    @PathVariable UUID jobRequestId) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(quoteService.getQuotesForRequest(userId, jobRequestId));
    }

    @GetMapping("/api/quotes/mine")
    @PreAuthorize("hasRole('GARAGE')")
    public ResponseEntity<List<QuoteResponse>> getMyQuotes(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(quoteService.getMyQuotes(userId));
    }
}
