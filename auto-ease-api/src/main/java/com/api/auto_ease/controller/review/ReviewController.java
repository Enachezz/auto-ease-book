package com.api.auto_ease.controller.review;

import com.api.auto_ease.dto.review.CreateReviewReplyRequest;
import com.api.auto_ease.dto.review.CreateReviewRequest;
import com.api.auto_ease.dto.review.ReviewReplyResponse;
import com.api.auto_ease.dto.review.ReviewResponse;
import com.api.auto_ease.service.review.ReviewReplyService;
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

import static com.api.auto_ease.security.AppUserTypeSecurityExpressions.HAS_ROLE_CAR_OWNER;
import static com.api.auto_ease.security.AppUserTypeSecurityExpressions.HAS_ROLE_GARAGE_OR_CAR_OWNER;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewReplyService reviewReplyService;

    @PostMapping("/api/job-requests/{jobRequestId}/reviews")
    @PreAuthorize(HAS_ROLE_CAR_OWNER)
    public ResponseEntity<ReviewResponse> createReview(Authentication auth,
                                                       @PathVariable UUID jobRequestId,
                                                       @Valid @RequestBody CreateReviewRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(userId, jobRequestId, request));
    }

    @GetMapping("/api/garages/{garageId}/reviews")
    public List<ReviewResponse> getReviewsForGarage(@PathVariable UUID garageId) {
        return reviewService.getReviewsForGarage(garageId);
    }

    @PostMapping("/api/reviews/{reviewId}/replies")
    @PreAuthorize(HAS_ROLE_GARAGE_OR_CAR_OWNER)
    public ResponseEntity<ReviewReplyResponse> createReply(Authentication auth,
                                                           @PathVariable UUID reviewId,
                                                           @Valid @RequestBody CreateReviewReplyRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewReplyService.createReply(userId, reviewId, request));
    }
}
