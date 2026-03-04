package com.api.auto_ease.service.quote;

import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.domain.jobrequest.JobRequest;
import com.api.auto_ease.domain.jobrequest.JobRequestStatus;
import com.api.auto_ease.domain.quote.Quote;
import com.api.auto_ease.domain.quote.QuoteStatus;
import com.api.auto_ease.dto.quote.CreateQuoteRequest;
import com.api.auto_ease.dto.quote.QuoteResponse;
import com.api.auto_ease.repository.garage.GarageRepository;
import com.api.auto_ease.repository.jobrequest.JobRequestRepository;
import com.api.auto_ease.repository.quote.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final JobRequestRepository jobRequestRepository;
    private final GarageRepository garageRepository;

    @Transactional
    public QuoteResponse submitQuote(String garageUserId, UUID jobRequestId, CreateQuoteRequest request) {
        Garage garage = garageRepository.findByUserId(garageUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must create a garage profile first"));

        JobRequest jobRequest = jobRequestRepository.findById(jobRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job request not found"));

        if (jobRequest.getStatus() != JobRequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job request is not open for quotes");
        }

        if (quoteRepository.existsByJobRequestIdAndGarageId(jobRequestId, garage.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already submitted a quote for this job request");
        }

        Quote quote = new Quote(null, jobRequestId, garage.getId(),
                request.getPrice(), request.getEstimatedDuration(),
                request.getDescription(), request.getWarrantyInfo(),
                QuoteStatus.PENDING, null, null, null);

        quote = quoteRepository.save(quote);
        return toResponse(quote, garage);
    }

    public List<QuoteResponse> getQuotesForRequest(String ownerUserId, UUID jobRequestId) {
        JobRequest jobRequest = jobRequestRepository.findById(jobRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job request not found"));

        if (!jobRequest.getUserId().equals(ownerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this job request");
        }

        return quoteRepository.findByJobRequestIdOrderByCreatedDateDesc(jobRequestId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<QuoteResponse> getMyQuotes(String garageUserId) {
        Garage garage = garageRepository.findByUserId(garageUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));

        return quoteRepository.findByGarageIdOrderByCreatedDateDesc(garage.getId()).stream()
                .map(q -> toResponse(q, garage))
                .toList();
    }

    private QuoteResponse toResponse(Quote quote) {
        Garage garage = garageRepository.findById(quote.getGarageId()).orElse(null);
        return toResponse(quote, garage);
    }

    private QuoteResponse toResponse(Quote quote, Garage garage) {
        return QuoteResponse.builder()
                .id(quote.getId())
                .jobRequestId(quote.getJobRequestId())
                .garageId(quote.getGarageId())
                .garageName(garage != null ? garage.getBusinessName() : "Unknown")
                .garageCity(garage != null ? garage.getCity() : null)
                .garageRating(garage != null ? garage.getAverageRating() : null)
                .price(quote.getPrice())
                .estimatedDuration(quote.getEstimatedDuration())
                .description(quote.getDescription())
                .warrantyInfo(quote.getWarrantyInfo())
                .status(quote.getStatus().name())
                .build();
    }
}
