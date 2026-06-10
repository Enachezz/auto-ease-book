package com.api.auto_ease.service.garage;

import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.domain.quote.Quote;
import com.api.auto_ease.domain.quote.QuoteStatus;
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
public class GarageJobAuthorization {

    private final GarageRepository garageRepository;
    private final JobRequestRepository jobRequestRepository;
    private final QuoteRepository quoteRepository;
    private final GarageService garageService;

    public void assertGarageUserMayMutateJob(String garageUserId, UUID jobRequestId) {
        Garage garage = garageRepository.findByUserId(garageUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Garage profile required"));
        garageService.assertGarageIsApproved(garage);

        jobRequestRepository.findById(jobRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job request not found"));

        List<Quote> acceptedQuotes = quoteRepository.findByJobRequestIdAndStatus(jobRequestId, QuoteStatus.ACCEPTED);
        boolean ownsJob = acceptedQuotes.stream()
                .anyMatch(quote -> quote.getGarageId().equals(garage.getId()));
        if (!ownsJob) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this job request");
        }

    }
}
