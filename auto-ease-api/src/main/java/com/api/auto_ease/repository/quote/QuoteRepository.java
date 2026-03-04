package com.api.auto_ease.repository.quote;

import com.api.auto_ease.domain.quote.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    List<Quote> findByJobRequestIdOrderByCreatedDateDesc(UUID jobRequestId);

    List<Quote> findByGarageIdOrderByCreatedDateDesc(UUID garageId);

    boolean existsByJobRequestIdAndGarageId(UUID jobRequestId, UUID garageId);

    int countByJobRequestId(UUID jobRequestId);
}
