package com.api.auto_ease.service.quoteLog;

import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.domain.jobrequest.JobRequest;
import com.api.auto_ease.domain.quote.Quote;
import com.api.auto_ease.domain.quote.QuoteStatus;
import com.api.auto_ease.domain.quoteLog.QuoteLog;
import com.api.auto_ease.domain.quoteLog.QuoteLogType;
import com.api.auto_ease.dto.quote.CreateQuoteRequest;
import com.api.auto_ease.dto.quoteLog.CreateQuoteLogRequest;
import com.api.auto_ease.dto.quoteLog.PagedQuoteLogsResponse;
import com.api.auto_ease.dto.quoteLog.QuoteLogResponse;
import com.api.auto_ease.repository.garage.GarageRepository;
import com.api.auto_ease.repository.jobrequest.JobRequestRepository;
import com.api.auto_ease.repository.quote.QuoteRepository;
import com.api.auto_ease.repository.quoteLog.QuoteLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuoteLogService {

    private static final int PAGE_SIZE = 20;

    private final QuoteLogRepository quoteLogRepository;
    private final QuoteRepository quoteRepository;
    private final JobRequestRepository jobRequestRepository;
    private final GarageRepository garageRepository;

    @Transactional
    public QuoteLogResponse createLog(String garageUserId, UUID quoteId, CreateQuoteLogRequest request) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found"));

        Garage garage = garageRepository.findByUserId(garageUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Garage profile required"));

        if (!quote.getGarageId().equals(garage.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own the garage on this quote");
        }

        JobRequest jobRequest = jobRequestRepository.findById(quote.getJobRequestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job request not found"));

        boolean updateFlag = Boolean.TRUE.equals(request.getUpdateFlag());
        UUID triggeredQuoteId = null;
        if (updateFlag) {
            Quote addendum = generateQuoteAddendum(request, jobRequest, garage);
            addendum = quoteRepository.save(addendum);
            triggeredQuoteId = addendum.getId();
        }

        QuoteLog log = new QuoteLog();
        log.setQuoteId(quote.getId());
        log.setAuthorUserId(garageUserId);
        log.setMessage(request.getMessage());
        log.setTypeOfNotification(QuoteLogType.CAR_OWNER);
        log.setReference(jobRequest.getUserId());
        log.setNotificationFlag(request.getNotificationFlag() == null
                ? Boolean.TRUE
                : request.getNotificationFlag());
        log.setTriggeredQuoteId(triggeredQuoteId);
        log = quoteLogRepository.save(log);

        return toResponse(log);
    }

    private Quote generateQuoteAddendum(CreateQuoteLogRequest request, JobRequest jobRequest, Garage garage) {
        CreateQuoteRequest newQuoteData = request.getNewQuote();
        if (newQuoteData == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "newQuote is required when updateFlag is true");
        }
        if (newQuoteData.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "newQuote.price is required when updateFlag is true");
        }
        return new Quote(null,
                jobRequest.getId(),
                garage.getId(),
                newQuoteData.getPrice(),
                newQuoteData.getEstimatedDuration(),
                newQuoteData.getDescription(),
                newQuoteData.getWarrantyInfo(),
                QuoteStatus.PENDING,
                null,
                null,
                null);
    }

    @Transactional(readOnly = true)
    public PagedQuoteLogsResponse listForCurrentUser(Authentication auth, Boolean notificationFlag, int page) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
        }

        Recipient recipient = resolveRecipient(auth);
        boolean onlyUnread = Boolean.TRUE.equals(notificationFlag);

        Sort sort = onlyUnread
                ? Sort.by(Sort.Direction.ASC, "createdDate")
                : Sort.by(Sort.Direction.DESC, "createdDate");
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, sort);

        Page<QuoteLog> result = onlyUnread
                ? quoteLogRepository.findByTypeOfNotificationAndReferenceAndNotificationFlag(
                        recipient.type, recipient.reference, Boolean.TRUE, pageable)
                : quoteLogRepository.findByTypeOfNotificationAndReference(
                        recipient.type, recipient.reference, pageable);

        return PagedQuoteLogsResponse.builder()
                .logs(result.getContent().stream().map(this::toResponse).toList())
                .totalCount(result.getTotalElements())
                .page(result.getNumber())
                .pageSize(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public QuoteLogResponse markAsRead(Authentication auth, UUID logId) {
        QuoteLog log = quoteLogRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote log not found"));

        Recipient recipient = resolveRecipient(auth);
        if (log.getTypeOfNotification() != recipient.type || !log.getReference().equals(recipient.reference)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the recipient of this log");
        }

        log.setNotificationFlag(Boolean.FALSE);
        log = quoteLogRepository.save(log);
        return toResponse(log);
    }

    @Transactional
    public void recordCarOwnerDecision(Quote quote, QuoteStatus decision, String carOwnerUserId) {
        if (decision != QuoteStatus.ACCEPTED && decision != QuoteStatus.REJECTED) {
            throw new IllegalArgumentException("decision must be ACCEPTED or REJECTED");
        }

        Optional<QuoteLog> originating = quoteLogRepository
                .findFirstByTriggeredQuoteIdAndTypeOfNotificationAndNotificationFlag(
                        quote.getId(), QuoteLogType.CAR_OWNER, Boolean.TRUE);
        originating.ifPresent(log -> {
            log.setNotificationFlag(Boolean.FALSE);
            quoteLogRepository.save(log);
        });

        if (originating.isEmpty()) {
            return;
        }

        QuoteLog reply = new QuoteLog();
        reply.setQuoteId(quote.getId());
        reply.setAuthorUserId(carOwnerUserId);
        reply.setMessage(decision == QuoteStatus.ACCEPTED
                ? "Car owner accepted the quote"
                : "Car owner rejected the quote");
        reply.setTypeOfNotification(QuoteLogType.GARAGE_OWNER);
        reply.setReference(quote.getGarageId().toString());
        reply.setNotificationFlag(Boolean.TRUE);
        reply.setTriggeredQuoteId(null);
        quoteLogRepository.save(reply);
    }

    private Recipient resolveRecipient(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        boolean isCarOwner = hasAuthority(auth, "ROLE_CAR_OWNER");
        boolean isGarage = hasAuthority(auth, "ROLE_GARAGE");

        if (isCarOwner && !isGarage) {
            return new Recipient(QuoteLogType.CAR_OWNER, userId);
        }
        if (isGarage) {
            Garage garage = garageRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Garage profile required"));
            return new Recipient(QuoteLogType.GARAGE_OWNER, garage.getId().toString());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only CAR_OWNER or GARAGE users have quote logs");
    }

    private static boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private QuoteLogResponse toResponse(QuoteLog log) {
        return QuoteLogResponse.builder()
                .id(log.getId())
                .quoteId(log.getQuoteId())
                .authorUserId(log.getAuthorUserId())
                .message(log.getMessage())
                .typeOfNotification(log.getTypeOfNotification().name())
                .reference(log.getReference())
                .notificationFlag(log.getNotificationFlag())
                .triggeredQuoteId(log.getTriggeredQuoteId())
                .createdDate(log.getCreatedDate())
                .modifiedDate(log.getModifiedDate())
                .build();
    }

    private record Recipient(QuoteLogType type, String reference) {
    }
}
