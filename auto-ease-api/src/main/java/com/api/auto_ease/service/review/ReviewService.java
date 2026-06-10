package com.api.auto_ease.service.review;

import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.domain.jobrequest.JobRequest;
import com.api.auto_ease.domain.jobrequest.JobRequestStatus;
import com.api.auto_ease.domain.quote.Quote;
import com.api.auto_ease.domain.quote.QuoteStatus;
import com.api.auto_ease.domain.review.Review;
import com.api.auto_ease.domain.review.ReviewReply;
import com.api.auto_ease.dto.review.CreateReviewRequest;
import com.api.auto_ease.dto.review.ReviewReplyResponse;
import com.api.auto_ease.dto.review.ReviewResponse;
import com.api.auto_ease.repository.garage.GarageRepository;
import com.api.auto_ease.repository.jobrequest.JobRequestRepository;
import com.api.auto_ease.repository.quote.QuoteRepository;
import com.api.auto_ease.repository.review.ReviewReplyRepository;
import com.api.auto_ease.repository.review.ReviewRepository;
import com.api.auto_ease.service.garage.GarageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final JobRequestRepository jobRequestRepository;
    private final QuoteRepository quoteRepository;
    private final GarageRepository garageRepository;
    private final GarageService garageService;

    @Transactional
    public ReviewResponse createReview(String userId, UUID jobRequestId, CreateReviewRequest request) {
        JobRequest jobRequest = jobRequestRepository.findById(jobRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job request not found"));

        if (!jobRequest.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this job request");
        }

        if (jobRequest.getStatus() != JobRequestStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job request is not completed");
        }

        if (reviewRepository.existsByJobRequestId(jobRequestId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review already exists for this job request");
        }

        UUID garageId = resolveGarageIdForJob(jobRequestId);

        Review review = new Review(null, jobRequestId, garageId, userId,
                request.getRating(), request.getComment(), null, null);
        review = reviewRepository.save(review);

        updateGarageRating(garageId);

        return toResponse(review, List.of());
    }

    public List<ReviewResponse> getReviewsForGarage(UUID garageId) {
        garageService.getApprovedGarageById(garageId);
        return reviewRepository.findByGarageIdOrderByCreatedDateDesc(garageId).stream()
                .map(review -> {
                    List<ReviewReply> replies = reviewReplyRepository.findByReviewIdOrderByCreatedDateAsc(review.getId());
                    return toResponse(review, replies);
                })
                .toList();
    }

    private UUID resolveGarageIdForJob(UUID jobRequestId) {
        List<Quote> acceptedQuotes = quoteRepository.findByJobRequestIdAndStatus(jobRequestId, QuoteStatus.ACCEPTED);
        if (acceptedQuotes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No accepted quotes for this job request");
        }
        return acceptedQuotes.get(0).getGarageId();
    }

    private void updateGarageRating(UUID garageId) {
        Garage garage = garageRepository.findById(garageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));

        List<Review> reviews = reviewRepository.findByGarageIdOrderByCreatedDateDesc(garageId);
        int totalReviews = reviews.size();
        if (totalReviews == 0) {
            garage.setTotalReviews(0);
            garage.setAverageRating(BigDecimal.ZERO);
        } else {
            BigDecimal avgRating = reviews.stream()
                    .map(review -> BigDecimal.valueOf(review.getRating()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(totalReviews), 2, RoundingMode.HALF_UP);
            garage.setTotalReviews(totalReviews);
            garage.setAverageRating(avgRating);
        }
        garageRepository.save(garage);
    }

    ReviewResponse toResponse(Review review, List<ReviewReply> replies) {
        Garage garage = garageRepository.findById(review.getGarageId()).orElse(null);
        String garageOwnerUserId = garage != null ? garage.getUserId() : null;

        List<ReviewReplyResponse> replyResponses = replies.stream()
                .map(reply -> toReplyResponse(reply, garageOwnerUserId, review.getUserId()))
                .toList();

        return ReviewResponse.builder()
                .id(review.getId())
                .jobRequestId(review.getJobRequestId())
                .garageId(review.getGarageId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdDate(review.getCreatedDate())
                .replies(replyResponses)
                .build();
    }

    private ReviewReplyResponse toReplyResponse(ReviewReply reply, String garageOwnerUserId, String reviewAuthorUserId) {
        String authorRole;
        if (reply.getAuthorUserId().equals(reviewAuthorUserId)) {
            authorRole = "CAR_OWNER";
        } else if (garageOwnerUserId != null && reply.getAuthorUserId().equals(garageOwnerUserId)) {
            authorRole = "GARAGE";
        } else {
            authorRole = "UNKNOWN";
        }

        return ReviewReplyResponse.builder()
                .id(reply.getId())
                .authorUserId(reply.getAuthorUserId())
                .authorRole(authorRole)
                .message(reply.getMessage())
                .parentReplyId(reply.getParentReplyId())
                .createdDate(reply.getCreatedDate())
                .build();
    }
}
