package com.api.auto_ease.controller.garage;

import com.api.auto_ease.domain.serviceEntry.ServiceEntry;
import com.api.auto_ease.dto.garage.CreateGarageRequest;
import com.api.auto_ease.dto.garage.GarageResponse;
import com.api.auto_ease.dto.garage.GarageSearchRequest;
import com.api.auto_ease.dto.garage.PagedGaragesResponse;
import com.api.auto_ease.dto.garage.SetGarageCategoriesRequest;
import com.api.auto_ease.dto.garage.SetGarageMakesRequest;
import com.api.auto_ease.dto.garage.UpdateGarageRequest;
import com.api.auto_ease.dto.referencedata.CarMakeResponse;
import com.api.auto_ease.dto.referencedata.ServiceCategoryResponse;
import com.api.auto_ease.service.garage.GarageService;
import com.api.auto_ease.service.serviceEntry.ServiceEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.api.auto_ease.security.AppUserTypeSecurityExpressions.HAS_ROLE_ADMIN;
import static com.api.auto_ease.security.AppUserTypeSecurityExpressions.HAS_ROLE_GARAGE;
import static com.api.auto_ease.security.AppUserTypeSecurityExpressions.HAS_ROLE_GARAGE_OR_ADMIN;

@RestController
@RequiredArgsConstructor
public class GarageController {

    private final GarageService garageService;
    private final ServiceEntryService serviceEntryService;

    @PostMapping("/api/garages")
    @PreAuthorize(HAS_ROLE_GARAGE)
    public ResponseEntity<GarageResponse> createGarage(Authentication auth,
                                                       @Valid @RequestBody CreateGarageRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(garageService.createGarage(userId, request));
    }

    @GetMapping("/api/garages/me")
    @PreAuthorize(HAS_ROLE_GARAGE)
    public GarageResponse getMyGarage(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return garageService.getMyGarage(userId);
    }

    @PutMapping("/api/garages/me")
    @PreAuthorize(HAS_ROLE_GARAGE)
    public GarageResponse updateMyGarage(Authentication auth,
                                         @RequestBody UpdateGarageRequest request) {
        String userId = (String) auth.getPrincipal();
        return garageService.updateGarage(userId, request);
    }

    @GetMapping("/api/garages/public/{id}")
    public GarageResponse getPublicGarage(@PathVariable UUID id) {
        return garageService.getPublicGarageById(id);
    }

    @PostMapping("/api/garages/search")
    public PagedGaragesResponse searchGarages(@Valid @RequestBody(required = false) GarageSearchRequest request) {
        GarageSearchRequest body = request != null ? request : new GarageSearchRequest();
        return garageService.searchApprovedGarages(body);
    }

    @GetMapping("/api/garages/me/categories")
    @PreAuthorize(HAS_ROLE_GARAGE)
    public List<ServiceCategoryResponse> getMyAcceptedCategories(Authentication auth) {
        return garageService.getAcceptedCategoriesForGarageUser((String) auth.getPrincipal());
    }

    @PutMapping("/api/garages/me/categories")
    @PreAuthorize(HAS_ROLE_GARAGE)
    public ResponseEntity<Void> replaceMyAcceptedCategories(Authentication auth,
                                                            @Valid @RequestBody SetGarageCategoriesRequest request) {
        garageService.replaceAcceptedCategoriesForGarageUser((String) auth.getPrincipal(), request.getCategoryIds());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/garages/me/makes")
    @PreAuthorize(HAS_ROLE_GARAGE_OR_ADMIN)
    public List<CarMakeResponse> getMyServiceableMakes(Authentication auth,
                                                       @RequestParam(required = false) UUID garageId) {
        return garageService.getServiceableMakes(auth, garageId);
    }

    @PutMapping("/api/garages/me/makes")
    @PreAuthorize(HAS_ROLE_GARAGE_OR_ADMIN)
    public ResponseEntity<Void> replaceMyServiceableMakes(Authentication auth,
                                                          @Valid @RequestBody SetGarageMakesRequest request) {
        garageService.replaceServiceableMakes(auth, request.getGarageId(), request.getMakeIds());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/garages")
    public List<GarageResponse> listApprovedGarages() {
        return garageService.listApprovedGarages();
    }

    @PatchMapping("/api/garages/{id}/approve")
    @PreAuthorize(HAS_ROLE_ADMIN)
    public GarageResponse approveGarage(@PathVariable UUID id) {
        return garageService.approveGarage(id);
    }

    @PostMapping("/api/v1/processServiceEntry")
    public ServiceEntry processServiceEntry(String uuid, Object payload) {
        return serviceEntryService.processServiceEntry(uuid, payload);
    }
}
