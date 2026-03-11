package com.api.auto_ease.controller.booking;

import com.api.auto_ease.dto.booking.AcceptQuoteRequest;
import com.api.auto_ease.dto.booking.BookingResponse;
import com.api.auto_ease.service.booking.BookingService;
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
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/api/quotes/{quoteId}/accept")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public ResponseEntity<BookingResponse> acceptQuote(Authentication auth,
                                                       @PathVariable UUID quoteId,
                                                       @RequestBody(required = false) AcceptQuoteRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.acceptQuote(userId, quoteId, request));
    }

    @GetMapping("/api/bookings")
    public List<BookingResponse> getMyBookings(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return bookingService.getMyBookings(userId);
    }
}
