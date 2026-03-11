package com.api.auto_ease.service.booking;

import com.api.auto_ease.domain.booking.Booking;
import com.api.auto_ease.domain.booking.BookingStatus;
import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.domain.jobrequest.JobRequest;
import com.api.auto_ease.domain.jobrequest.JobRequestStatus;
import com.api.auto_ease.domain.quote.Quote;
import com.api.auto_ease.domain.quote.QuoteStatus;
import com.api.auto_ease.dto.booking.AcceptQuoteRequest;
import com.api.auto_ease.dto.booking.BookingResponse;
import com.api.auto_ease.repository.booking.BookingRepository;
import com.api.auto_ease.repository.garage.GarageRepository;
import com.api.auto_ease.repository.jobrequest.JobRequestRepository;
import com.api.auto_ease.repository.quote.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final QuoteRepository quoteRepository;
    private final JobRequestRepository jobRequestRepository;
    private final GarageRepository garageRepository;

    @Transactional
    public BookingResponse acceptQuote(String ownerUserId, UUID quoteId, AcceptQuoteRequest request) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found"));

        if (quote.getStatus() != QuoteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quote is not pending");
        }

        JobRequest jobRequest = jobRequestRepository.findById(quote.getJobRequestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job request not found"));

        if (!jobRequest.getUserId().equals(ownerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this job request");
        }

        if (jobRequest.getStatus() != JobRequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job request is no longer open");
        }

        quote.setStatus(QuoteStatus.ACCEPTED);
        quoteRepository.save(quote);

        List<Quote> allQuotes = quoteRepository.findByJobRequestId(jobRequest.getId());
        List<Quote> toReject = allQuotes.stream()
                .filter(otherQuote -> !otherQuote.getId().equals(quoteId))
                .peek(otherQuote -> otherQuote.setStatus(QuoteStatus.REJECTED))
                .toList();
        quoteRepository.saveAll(toReject);

        jobRequest.setStatus(JobRequestStatus.BOOKED);
        jobRequestRepository.save(jobRequest);

        Booking booking = new Booking(null, quoteId,
                request != null ? request.getScheduledDate() : null,
                request != null ? request.getScheduledTime() : null,
                BookingStatus.CONFIRMED,
                request != null ? request.getNotes() : null,
                null, null);
        booking = bookingRepository.save(booking);

        Garage garage = garageRepository.findById(quote.getGarageId()).orElse(null);
        return toResponse(booking, quote, jobRequest, garage);
    }

    public List<BookingResponse> getMyBookings(String userId) {
        List<BookingResponse> ownerBookings = getOwnerBookings(userId);
        List<BookingResponse> garageBookings = getGarageBookings(userId);

        Map<UUID, BookingResponse> merged = new LinkedHashMap<>();
        for (BookingResponse bookingResponse : ownerBookings) merged.put(bookingResponse.getId(), bookingResponse);
        for (BookingResponse bookingResponse : garageBookings) merged.put(bookingResponse.getId(), bookingResponse);

        return new ArrayList<>(merged.values());
    }

    private List<BookingResponse> getOwnerBookings(String userId) {
        List<JobRequest> jobRequests = jobRequestRepository.findByUserIdOrderByCreatedDateDesc(userId);
        if (jobRequests.isEmpty()) return List.of();

        List<UUID> quoteIds = jobRequests.stream()
                .flatMap(jobRequest -> quoteRepository.findByJobRequestId(jobRequest.getId()).stream())
                .map(Quote::getId)
                .toList();
        if (quoteIds.isEmpty()) return List.of();

        Map<UUID, JobRequest> jobRequestByQuoteId = new HashMap<>();
        Map<UUID, Quote> quoteById = new HashMap<>();
        for (JobRequest jobRequest : jobRequests) {
            for (Quote quote : quoteRepository.findByJobRequestId(jobRequest.getId())) {
                quoteById.put(quote.getId(), quote);
                jobRequestByQuoteId.put(quote.getId(), jobRequest);
            }
        }

        return bookingRepository.findByQuoteIdIn(quoteIds).stream()
                .map(booking -> {
                    Quote quote = quoteById.get(booking.getQuoteId());
                    JobRequest jobRequest = jobRequestByQuoteId.get(booking.getQuoteId());
                    Garage garage = quote != null ? garageRepository.findById(quote.getGarageId()).orElse(null) : null;
                    return toResponse(booking, quote, jobRequest, garage);
                })
                .toList();
    }

    private List<BookingResponse> getGarageBookings(String userId) {
        Optional<Garage> garageOpt = garageRepository.findByUserId(userId);
        if (garageOpt.isEmpty()) return List.of();

        Garage garage = garageOpt.get();
        List<Quote> quotes = quoteRepository.findByGarageIdOrderByCreatedDateDesc(garage.getId());
        if (quotes.isEmpty()) return List.of();

        List<UUID> quoteIds = quotes.stream().map(Quote::getId).toList();
        Map<UUID, Quote> quoteById = new HashMap<>();
        quotes.forEach(quote -> quoteById.put(quote.getId(), quote));

        return bookingRepository.findByQuoteIdIn(quoteIds).stream()
                .map(booking -> {
                    Quote quote = quoteById.get(booking.getQuoteId());
                    JobRequest jobRequest = quote != null ? jobRequestRepository.findById(quote.getJobRequestId()).orElse(null) : null;
                    return toResponse(booking, quote, jobRequest, garage);
                })
                .toList();
    }

    private BookingResponse toResponse(Booking booking, Quote quote, JobRequest jobRequest, Garage garage) {
        return BookingResponse.builder()
                .id(booking.getId())
                .quoteId(booking.getQuoteId())
                .garageId(quote != null ? quote.getGarageId() : null)
                .garageName(garage != null ? garage.getBusinessName() : "Unknown")
                .jobTitle(jobRequest != null ? jobRequest.getTitle() : "Unknown")
                .price(quote != null ? quote.getPrice() : null)
                .scheduledDate(booking.getScheduledDate())
                .scheduledTime(booking.getScheduledTime())
                .status(booking.getStatus().name())
                .notes(booking.getNotes())
                .build();
    }
}
