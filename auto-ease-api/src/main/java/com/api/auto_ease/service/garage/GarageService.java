package com.api.auto_ease.service.garage;

import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.dto.garage.CreateGarageRequest;
import com.api.auto_ease.dto.garage.GarageResponse;
import com.api.auto_ease.dto.garage.UpdateGarageRequest;
import com.api.auto_ease.repository.garage.GarageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GarageService {

    private final GarageRepository garageRepository;

    @Transactional
    public GarageResponse createGarage(String userId, CreateGarageRequest request) {
        if (garageRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has a garage");
        }

        Garage garage = new Garage(null, userId, request.getBusinessName(),
                request.getAddress(), request.getCity(), request.getState(),
                request.getPostalCode(), request.getPhone(), request.getDescription(),
                request.getServices(), false, null, null, null, null);

        garage = garageRepository.save(garage);
        return toResponse(garage);
    }

    public GarageResponse getMyGarage(String userId) {
        Garage garage = garageRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));
        return toResponse(garage);
    }

    @Transactional
    public GarageResponse updateGarage(String userId, UpdateGarageRequest request) {
        Garage garage = garageRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));

        if (request.getBusinessName() != null) garage.setBusinessName(request.getBusinessName());
        if (request.getAddress() != null) garage.setAddress(request.getAddress());
        if (request.getCity() != null) garage.setCity(request.getCity());
        if (request.getState() != null) garage.setState(request.getState());
        if (request.getPostalCode() != null) garage.setPostalCode(request.getPostalCode());
        if (request.getPhone() != null) garage.setPhone(request.getPhone());
        if (request.getDescription() != null) garage.setDescription(request.getDescription());
        if (request.getServices() != null) garage.setServices(request.getServices());

        garage = garageRepository.save(garage);
        return toResponse(garage);
    }

    public List<GarageResponse> listApprovedGarages() {
        return garageRepository.findByIsApprovedTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GarageResponse approveGarage(java.util.UUID garageId) {
        Garage garage = garageRepository.findById(garageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));
        garage.setIsApproved(true);
        garage = garageRepository.save(garage);
        return toResponse(garage);
    }

    private GarageResponse toResponse(Garage garage) {
        return GarageResponse.builder()
                .id(garage.getId())
                .userId(garage.getUserId())
                .businessName(garage.getBusinessName())
                .address(garage.getAddress())
                .city(garage.getCity())
                .state(garage.getState())
                .postalCode(garage.getPostalCode())
                .phone(garage.getPhone())
                .description(garage.getDescription())
                .services(garage.getServices())
                .isApproved(garage.getIsApproved())
                .averageRating(garage.getAverageRating())
                .totalReviews(garage.getTotalReviews())
                .build();
    }
}
