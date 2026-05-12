package com.api.auto_ease.service.calendar;

import com.api.auto_ease.domain.booking.Booking;
import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.dto.calendar.CalendarResponse;
import com.api.auto_ease.repository.booking.BookingRepository;
import com.api.auto_ease.repository.garage.GarageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final GarageRepository garageRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<CalendarResponse> getPublicCalendarForGarage(UUID garageId, Integer year, Integer month) {
        Garage garage = garageRepository.findById(garageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));
        if (!Boolean.TRUE.equals(garage.getIsApproved())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found");
        }
        return loadEntries(garage.getId(), parseMonthFilter(year, month));
    }

    @Transactional(readOnly = true)
    public List<CalendarResponse> getMyCalendar(String garageUserId) {
        Garage garage = garageRepository.findByUserId(garageUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));
        return loadEntries(garage.getId(), null);
    }

    private List<CalendarResponse> loadEntries(UUID garageId, YearMonth monthFilter) {
        List<Booking> bookings = monthFilter == null
                ? bookingRepository.findByGarageId(garageId)
                : bookingRepository.findByGarageIdAndScheduledDateBetween(
                        garageId,
                        monthFilter.atDay(1),
                        monthFilter.atEndOfMonth());
        return bookings.stream()
                .map(this::toResponse)
                .toList();
    }

    private YearMonth parseMonthFilter(Integer year, Integer month) {
        if (year == null && month == null) {
            return null;
        }
        if (year == null || month == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Both 'year' and 'month' must be provided together");
        }
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid year/month: " + e.getMessage());
        }
    }

    private CalendarResponse toResponse(Booking booking) {
        return CalendarResponse.builder()
                .id(booking.getId())
                .garageId(booking.getGarageId())
                .customerId(booking.getCustomerId())
                .scheduledDate(booking.getScheduledDate())
                .scheduledTime(booking.getScheduledTime())
                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .description(booking.getNotes())
                .createdDate(booking.getCreatedDate())
                .modifiedDate(booking.getModifiedDate())
                .build();
    }
}
