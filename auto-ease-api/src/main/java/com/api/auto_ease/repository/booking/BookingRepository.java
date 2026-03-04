package com.api.auto_ease.repository.booking;

import com.api.auto_ease.domain.booking.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByQuoteId(UUID quoteId);

    List<Booking> findByQuoteIdIn(List<UUID> quoteIds);

    boolean existsByQuoteId(UUID quoteId);
}
