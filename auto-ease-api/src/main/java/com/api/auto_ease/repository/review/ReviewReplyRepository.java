package com.api.auto_ease.repository.review;

import com.api.auto_ease.domain.review.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, UUID> {

    List<ReviewReply> findByReviewIdOrderByCreatedDateAsc(UUID reviewId);

    Optional<ReviewReply> findFirstByReviewIdOrderByCreatedDateDesc(UUID reviewId);
}
