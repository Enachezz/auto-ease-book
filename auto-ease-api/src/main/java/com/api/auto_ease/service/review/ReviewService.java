package com.api.auto_ease.service.review;

import com.api.auto_ease.domain.booking.Booking;
import com.api.auto_ease.domain.booking.BookingStatus;
import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.domain.jobrequest.JobRequest;
import com.api.auto_ease.domain.quote.Quote;
import com.api.auto_ease.domain.review.Review;
import com.api.auto_ease.dto.review.CreateReviewRequest;
import com.api.auto_ease.dto.review.ReviewResponse;
import com.api.auto_ease.repository.booking.BookingRepository;
import com.api.auto_ease.repository.garage.GarageRepository;
import com.api.auto_ease.repository.jobrequest.JobRequestRepository;
import com.api.auto_ease.repository.quote.QuoteRepository;
import com.api.auto_ease.repository.review.ReviewRepository;
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
    private final BookingRepository bookingRepository;
    private final QuoteRepository quoteRepository;
    private final JobRequestRepository jobRequestRepository;
    private final GarageRepository garageRepository;

    @Transactional
    public ReviewResponse createReview(String userId, UUID bookingId, CreateReviewRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        Quote quote = quoteRepository.findById(booking.getQuoteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found"));

        JobRequest jobRequest = jobRequestRepository.findById(quote.getJobRequestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job request not found"));

        if (!jobRequest.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this booking");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is not completed");
        }

        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review already exists for this booking");
        }

        Review review = new Review(null, bookingId, quote.getGarageId(), userId,
                request.getRating(), request.getComment(), null, null);
        review = reviewRepository.save(review);

        updateGarageRating(quote.getGarageId());

        return toResponse(review);
    }

    public List<ReviewResponse> getReviewsForGarage(UUID garageId) {
        return reviewRepository.findByGarageIdOrderByCreatedDateDesc(garageId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void updateGarageRating(UUID garageId) {
        Garage garage = garageRepository.findById(garageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));

        List<Review> reviews = reviewRepository.findByGarageIdOrderByCreatedDateDesc(garageId);
        int totalReviews = reviews.size();
        BigDecimal avgRating = reviews.stream()
                .map(review -> BigDecimal.valueOf(review.getRating()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(totalReviews), 2, RoundingMode.HALF_UP);

        garage.setTotalReviews(totalReviews);
        garage.setAverageRating(avgRating);
        garageRepository.save(garage);
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBookingId())
                .garageId(review.getGarageId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .comment(review.getComment())
                .build();
    }
}
