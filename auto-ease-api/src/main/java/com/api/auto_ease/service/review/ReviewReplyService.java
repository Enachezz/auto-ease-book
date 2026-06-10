package com.api.auto_ease.service.review;

import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.domain.review.Review;
import com.api.auto_ease.domain.review.ReviewReply;
import com.api.auto_ease.dto.review.CreateReviewReplyRequest;
import com.api.auto_ease.dto.review.ReviewReplyResponse;
import com.api.auto_ease.repository.garage.GarageRepository;
import com.api.auto_ease.repository.review.ReviewReplyRepository;
import com.api.auto_ease.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewReplyService {

    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final GarageRepository garageRepository;

    @Transactional
    public ReviewReplyResponse createReply(String userId, UUID reviewId, CreateReviewReplyRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        Garage garage = garageRepository.findById(review.getGarageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));

        boolean isGarageOwner = garage.getUserId().equals(userId);
        boolean isReviewAuthor = review.getUserId().equals(userId);

        if (!isGarageOwner && !isReviewAuthor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to reply to this review");
        }

        Optional<ReviewReply> lastReplyOpt = reviewReplyRepository.findFirstByReviewIdOrderByCreatedDateDesc(reviewId);

        if (lastReplyOpt.isEmpty()) {
            if (!isGarageOwner) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the garage owner may post the first reply");
            }
            if (request.getParentReplyId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First reply must not have a parent");
            }
        } else {
            ReviewReply lastReply = lastReplyOpt.get();
            boolean lastWasGarage = lastReply.getAuthorUserId().equals(garage.getUserId());

            if (lastWasGarage) {
                if (!isReviewAuthor) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It is the car owner's turn to reply");
                }
            } else {
                if (!isGarageOwner) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It is the garage owner's turn to reply");
                }
            }

            if (request.getParentReplyId() == null || !request.getParentReplyId().equals(lastReply.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "parentReplyId must reference the latest reply");
            }
        }

        ReviewReply reply = new ReviewReply();
        reply.setReviewId(reviewId);
        reply.setParentReplyId(request.getParentReplyId());
        reply.setAuthorUserId(userId);
        reply.setMessage(request.getMessage());
        reply = reviewReplyRepository.save(reply);

        String authorRole = isGarageOwner ? "GARAGE" : "CAR_OWNER";
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
