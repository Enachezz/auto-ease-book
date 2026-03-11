package com.api.auto_ease.controller.review;

import com.api.auto_ease.dto.review.CreateReviewRequest;
import com.api.auto_ease.dto.review.ReviewResponse;
import com.api.auto_ease.service.review.ReviewService;
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
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/bookings/{bookingId}/reviews")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public ResponseEntity<ReviewResponse> createReview(Authentication auth,
                                                        @PathVariable UUID bookingId,
                                                        @Valid @RequestBody CreateReviewRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(userId, bookingId, request));
    }

    @GetMapping("/api/garages/{garageId}/reviews")
    public List<ReviewResponse> getReviewsForGarage(@PathVariable UUID garageId) {
        return reviewService.getReviewsForGarage(garageId);
    }
}
