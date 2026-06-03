package com.api.auto_ease.service.garage;

import com.api.auto_ease.domain.booking.Booking;
import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.repository.booking.BookingRepository;
import com.api.auto_ease.repository.garage.GarageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Central guard for garage-initiated booking mutations (accept, update, delete).
 * Call at the start of any such service method so approval and ownership stay consistent.
 */
@Service
@RequiredArgsConstructor
public class GarageBookingAuthorization {

    private final GarageRepository garageRepository;
    private final BookingRepository bookingRepository;
    private final GarageService garageService;

    @Transactional(readOnly = true)
    public Garage assertGarageUserMayMutateBooking(String garageUserId, UUID bookingId) {
        Garage garage = garageRepository.findByUserId(garageUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Garage profile required"));
        garageService.assertGarageIsApproved(garage);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!booking.getGarageId().equals(garage.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this booking");
        }
        return garage;
    }
}
