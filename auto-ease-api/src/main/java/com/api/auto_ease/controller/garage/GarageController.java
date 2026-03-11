package com.api.auto_ease.controller.garage;

import com.api.auto_ease.dto.garage.CreateGarageRequest;
import com.api.auto_ease.dto.garage.GarageResponse;
import com.api.auto_ease.dto.garage.UpdateGarageRequest;
import com.api.auto_ease.service.garage.GarageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GarageController {

    private final GarageService garageService;

    @PostMapping("/api/garages")
    @PreAuthorize("hasRole('GARAGE')")
    public ResponseEntity<GarageResponse> createGarage(Authentication auth,
                                                       @Valid @RequestBody CreateGarageRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(garageService.createGarage(userId, request));
    }

    @GetMapping("/api/garages/me")
    @PreAuthorize("hasRole('GARAGE')")
    public ResponseEntity<GarageResponse> getMyGarage(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(garageService.getMyGarage(userId));
    }

    @PutMapping("/api/garages/me")
    @PreAuthorize("hasRole('GARAGE')")
    public ResponseEntity<GarageResponse> updateMyGarage(Authentication auth,
                                                         @RequestBody UpdateGarageRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(garageService.updateGarage(userId, request));
    }

    @GetMapping("/api/garages")
    public ResponseEntity<List<GarageResponse>> listApprovedGarages() {
        return ResponseEntity.ok(garageService.listApprovedGarages());
    }

    @PatchMapping("/api/garages/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GarageResponse> approveGarage(@PathVariable UUID id) {
        return ResponseEntity.ok(garageService.approveGarage(id));
    }
}
