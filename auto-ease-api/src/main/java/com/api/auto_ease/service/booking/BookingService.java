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
import com.api.auto_ease.domain.profile.Profile;
import com.api.auto_ease.repository.booking.BookingRepository;
import com.api.auto_ease.repository.garage.GarageRepository;
import com.api.auto_ease.repository.jobrequest.JobRequestRepository;
import com.api.auto_ease.repository.profile.ProfileRepository;
import com.api.auto_ease.repository.quote.QuoteRepository;
import com.api.auto_ease.service.garage.GarageService;
import com.api.auto_ease.service.quoteLog.QuoteLogService;
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
    private final GarageService garageService;
    private final GarageRepository garageRepository;
    private final ProfileRepository profileRepository;
    private final QuoteLogService quoteLogService;

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

        garageService.getApprovedGarageById(quote.getGarageId());

        if (request != null && request.getAddendumFlow() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Request body with addendumFlow is required");
        }

        boolean addendumFlow = request != null && Boolean.TRUE.equals(request.getAddendumFlow());
        JobRequestStatus status = jobRequest.getStatus();
        if (addendumFlow) {
            if (status != JobRequestStatus.BOOKED && status != JobRequestStatus.IN_PROGRESS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "addendumFlow=true is only allowed when job request status is BOOKED or IN_PROGRESS");
            }
        } else {
            if (status != JobRequestStatus.OPEN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "addendumFlow=false is only allowed when job request status is OPEN");
            }
        }

        quote.setStatus(QuoteStatus.ACCEPTED);
        quoteRepository.save(quote);

        if (!addendumFlow) {
            List<Quote> allQuotes = quoteRepository.findByJobRequestId(jobRequest.getId());
            List<Quote> toReject = allQuotes.stream()
                    .filter(otherQuote -> !otherQuote.getId().equals(quoteId))
                    .peek(otherQuote -> otherQuote.setStatus(QuoteStatus.REJECTED))
                    .toList();
            quoteRepository.saveAll(toReject);

            jobRequest.setStatus(JobRequestStatus.BOOKED);
            jobRequestRepository.save(jobRequest);
        }

        Booking booking = new Booking();
        booking.setQuoteId(quoteId);
        booking.setGarageId(quote.getGarageId());
        booking.setCustomerId(jobRequest.getUserId());
        booking.setScheduledDate(request != null ? request.getScheduledDate() : null);
        booking.setScheduledTime(request != null ? request.getScheduledTime() : null);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setNotes(request != null ? request.getNotes() : null);
        booking = bookingRepository.save(booking);

        if (addendumFlow) {
            quoteLogService.recordCarOwnerDecision(quote, QuoteStatus.ACCEPTED, ownerUserId);
        }

        Garage garage = garageService.findApprovedGarageById(quote.getGarageId()).orElse(null);
        String clientPhone = profileRepository.findByUserId(jobRequest.getUserId())
                .map(Profile::getPhone)
                .orElse(null);
        return toResponse(booking, quote, jobRequest, garage, clientPhone);
    }

    public List<BookingResponse> getMyBookings(String userId) {
        List<BookingResponse> ownerBookings = getOwnerBookings(userId);
        List<BookingResponse> garageBookings = getGarageBookings(userId);

        Map<UUID, BookingResponse> merged = new LinkedHashMap<>();
        for (BookingResponse bookingResponse : ownerBookings) {
            merged.put(bookingResponse.getId(), bookingResponse);
        }
        for (BookingResponse bookingResponse : garageBookings) {
            merged.put(bookingResponse.getId(), bookingResponse);
        }

        return new ArrayList<>(merged.values());
    }

    private List<BookingResponse> getOwnerBookings(String userId) {
        List<JobRequest> jobRequests = jobRequestRepository.findByUserIdOrderByCreatedDateDesc(userId);
        if (jobRequests.isEmpty()) {
            return List.of();
        }

        List<UUID> quoteIds = jobRequests.stream()
                .flatMap(jobRequest -> quoteRepository.findByJobRequestId(jobRequest.getId()).stream())
                .map(Quote::getId)
                .toList();
        if (quoteIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, JobRequest> jobRequestByQuoteId = new HashMap<>();
        Map<UUID, Quote> quoteById = new HashMap<>();
        for (JobRequest jobRequest : jobRequests) {
            for (Quote quote : quoteRepository.findByJobRequestId(jobRequest.getId())) {
                quoteById.put(quote.getId(), quote);
                jobRequestByQuoteId.put(quote.getId(), jobRequest);
            }
        }

        String clientPhone = profileRepository.findByUserId(userId)
                .map(Profile::getPhone)
                .orElse(null);

        return bookingRepository.findByQuoteIdIn(quoteIds).stream()
                .map(booking -> {
                    Quote quote = quoteById.get(booking.getQuoteId());
                    JobRequest jobRequest = jobRequestByQuoteId.get(booking.getQuoteId());
                    Garage garage = quote != null
                            ? garageService.findApprovedGarageById(quote.getGarageId()).orElse(null)
                            : null;
                    return toResponse(booking, quote, jobRequest, garage, clientPhone);
                })
                .toList();
    }

    private List<BookingResponse> getGarageBookings(String userId) {
        Optional<Garage> garageOpt = garageRepository.findByUserId(userId);
        if (garageOpt.isEmpty()) {
            return List.of();
        }

        Garage garage = garageOpt.get();
        List<Quote> quotes = quoteRepository.findByGarageIdOrderByCreatedDateDesc(garage.getId());
        if (quotes.isEmpty()) {
            return List.of();
        }

        List<UUID> quoteIds = quotes.stream().map(Quote::getId).toList();
        Map<UUID, Quote> quoteById = new HashMap<>();
        quotes.forEach(quote -> quoteById.put(quote.getId(), quote));

        Map<UUID, JobRequest> jobRequestByQuoteId = new HashMap<>();
        for (Quote quote : quotes) {
            jobRequestRepository.findById(quote.getJobRequestId())
                    .ifPresent(jobRequest -> jobRequestByQuoteId.put(quote.getId(), jobRequest));
        }

        Map<String, String> clientPhoneByUserId = new HashMap<>();
        List<String> customerUserIds = jobRequestByQuoteId.values().stream()
                .map(JobRequest::getUserId)
                .distinct()
                .toList();
        if (!customerUserIds.isEmpty()) {
            profileRepository.findByUserIdIn(customerUserIds).forEach(profile ->
                    clientPhoneByUserId.put(profile.getUserId(), profile.getPhone()));
        }

        return bookingRepository.findByQuoteIdIn(quoteIds).stream()
                .map(booking -> {
                    Quote quote = quoteById.get(booking.getQuoteId());
                    JobRequest jobRequest = jobRequestByQuoteId.get(booking.getQuoteId());
                    String clientPhone = jobRequest != null
                            ? clientPhoneByUserId.get(jobRequest.getUserId())
                            : null;
                    return toResponse(booking, quote, jobRequest, garage, clientPhone);
                })
                .toList();
    }

    private BookingResponse toResponse(Booking booking, Quote quote, JobRequest jobRequest, Garage garage,
                                       String clientPhone) {
        return BookingResponse.builder()
                .id(booking.getId())
                .quoteId(booking.getQuoteId())
                .garageId(quote != null ? quote.getGarageId() : null)
                .garageName(garage != null ? garage.getBusinessName() : "Unknown")
                .garagePhone(garage != null ? garage.getPhone() : null)
                .clientPhone(clientPhone)
                .jobTitle(jobRequest != null ? jobRequest.getTitle() : "Unknown")
                .price(quote != null ? quote.getPrice() : null)
                .scheduledDate(booking.getScheduledDate())
                .scheduledTime(booking.getScheduledTime())
                .status(booking.getStatus().name())
                .notes(booking.getNotes())
                .build();
    }
}
