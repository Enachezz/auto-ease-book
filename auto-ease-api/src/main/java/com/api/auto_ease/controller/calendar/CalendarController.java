package com.api.auto_ease.controller.calendar;

import com.api.auto_ease.dto.calendar.CalendarResponse;
import com.api.auto_ease.service.calendar.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.api.auto_ease.security.AppUserTypeSecurityExpressions.HAS_ROLE_GARAGE;

@RestController
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/api/garages/public/{garageId}/calendar")
    public List<CalendarResponse> getPublicCalendar(@PathVariable UUID garageId,
                                                    @RequestParam(required = false) Integer year,
                                                    @RequestParam(required = false) Integer month) {
        return calendarService.getPublicCalendarForGarage(garageId, year, month);
    }

    @GetMapping("/api/garages/me/calendar")
    @PreAuthorize(HAS_ROLE_GARAGE)
    public List<CalendarResponse> getMyCalendar(Authentication auth) {
        return calendarService.getMyCalendar((String) auth.getPrincipal());
    }
}
