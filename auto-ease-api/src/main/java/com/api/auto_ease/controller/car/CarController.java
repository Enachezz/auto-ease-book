package com.api.auto_ease.controller.car;

import com.api.auto_ease.dto.car.CarResponse;
import com.api.auto_ease.dto.car.CreateCarRequest;
import com.api.auto_ease.dto.car.UpdateCarRequest;
import com.api.auto_ease.service.car.CarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @PostMapping("/api/cars")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public ResponseEntity<CarResponse> addCar(Authentication auth,
                                              @Valid @RequestBody CreateCarRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(carService.addCar(userId, request));
    }

    @GetMapping("/api/cars")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public List<CarResponse> getMyCars(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return carService.getMyCars(userId);
    }

    @PutMapping("/api/cars/{id}")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public CarResponse updateCar(Authentication auth,
                                 @PathVariable Integer id,
                                 @RequestBody UpdateCarRequest request) {
        String userId = (String) auth.getPrincipal();
        return carService.updateCar(userId, id, request);
    }

    @DeleteMapping("/api/cars/{id}")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public ResponseEntity<Void> deleteCar(Authentication auth,
                                          @PathVariable Integer id) {
        String userId = (String) auth.getPrincipal();
        carService.deleteCar(userId, id);
        return ResponseEntity.noContent().build();
    }
}
