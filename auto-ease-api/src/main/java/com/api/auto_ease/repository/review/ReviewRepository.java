package com.api.auto_ease.repository.review;

import com.api.auto_ease.domain.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByGarageIdOrderByCreatedDateDesc(UUID garageId);

    boolean existsByBookingId(UUID bookingId);
}
