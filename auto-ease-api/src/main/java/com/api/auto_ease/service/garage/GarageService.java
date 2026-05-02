package com.api.auto_ease.service.garage;

import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.domain.garageCategory.GarageCategoryAssignment;
import com.api.auto_ease.domain.serviceCategory.ServiceCategory;
import com.api.auto_ease.dto.garage.CreateGarageRequest;
import com.api.auto_ease.dto.garage.GarageFilterCriteria;
import com.api.auto_ease.dto.garage.GarageResponse;
import com.api.auto_ease.dto.garage.GarageSearchRequest;
import com.api.auto_ease.dto.garage.PagedGaragesResponse;
import com.api.auto_ease.dto.garage.UpdateGarageRequest;
import com.api.auto_ease.dto.referencedata.ServiceCategoryResponse;
import com.api.auto_ease.repository.garage.GarageRepository;
import com.api.auto_ease.repository.garageCategory.GarageCategoryAssignmentRepository;
import com.api.auto_ease.repository.serviceCategory.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GarageService {

    public static final int GARAGE_SEARCH_PAGE_SIZE = 20;

    private final GarageRepository garageRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final GarageCategoryAssignmentRepository garageCategoryAssignmentRepository;

    @Transactional
    public GarageResponse createGarage(String userId, CreateGarageRequest request) {
        if (garageRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has a garage");
        }

        Garage garage = new Garage();
        garage.setUserId(userId);
        garage.setBusinessName(request.getBusinessName());
        garage.setAddress(request.getAddress());
        garage.setCity(request.getCity());
        garage.setState(request.getState());
        garage.setPostalCode(request.getPostalCode());
        garage.setPhone(request.getPhone());
        garage.setDescription(request.getDescription());
        garage.setServices(request.getServices());
        garage.setEmail(request.getEmail());
        garage.setDealership(request.getDealership() != null && request.getDealership());
        garage.setIsApproved(false);
        garage.setAverageRating(BigDecimal.ZERO);
        garage.setTotalReviews(0);

        garage = garageRepository.save(garage);
        return toResponse(garage);
    }

    public GarageResponse getMyGarage(String userId) {
        Garage garage = garageRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));
        return toResponse(garage);
    }

    public GarageResponse getPublicGarageById(UUID id) {
        Garage garage = garageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));
        if (!Boolean.TRUE.equals(garage.getIsApproved())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found");
        }
        return toResponse(garage);
    }

    public PagedGaragesResponse searchApprovedGarages(GarageSearchRequest request) {
        GarageFilterCriteria filter = request.getFilter() != null
                ? request.getFilter()
                : GarageFilterCriteria.builder().build();
        Specification<Garage> spec = Specification
                .where(GarageSpecifications.fromFilter(filter))
                .and((root, query, cb) -> cb.isTrue(root.get("isApproved")));
        var pageable = PageRequest.of(request.getPage(), GARAGE_SEARCH_PAGE_SIZE, Sort.by("businessName").ascending());
        Page<Garage> page = garageRepository.findAll(spec, pageable);
        return PagedGaragesResponse.builder()
                .garages(page.getContent().stream().map(this::toResponse).toList())
                .totalCount(page.getTotalElements())
                .page(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Transactional
    public GarageResponse updateGarage(String userId, UpdateGarageRequest request) {
        Garage garage = garageRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));

        if (request.getBusinessName() != null) {
            garage.setBusinessName(request.getBusinessName());
        }
        if (request.getAddress() != null) {
            garage.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            garage.setCity(request.getCity());
        }
        if (request.getState() != null) {
            garage.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            garage.setPostalCode(request.getPostalCode());
        }
        if (request.getPhone() != null) {
            garage.setPhone(request.getPhone());
        }
        if (request.getDescription() != null) {
            garage.setDescription(request.getDescription());
        }
        if (request.getServices() != null) {
            garage.setServices(request.getServices());
        }
        if (request.getEmail() != null) {
            garage.setEmail(request.getEmail());
        }
        if (request.getDealership() != null) {
            garage.setDealership(request.getDealership());
        }

        garage = garageRepository.save(garage);
        return toResponse(garage);
    }

    public List<GarageResponse> listApprovedGarages() {
        return garageRepository.findByIsApprovedTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GarageResponse approveGarage(UUID garageId) {
        Garage garage = garageRepository.findById(garageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));
        garage.setIsApproved(true);
        garage = garageRepository.save(garage);
        return toResponse(garage);
    }

    public void assertGarageUserOwnsGarage(String garageUserId, UUID garageId) {
        if (garageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "garageId is required");
        }
        Garage garage = garageRepository.findByUserId(garageUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Garage profile required"));
        if (!garage.getId().equals(garageId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this garage");
        }
    }

    @Transactional(readOnly = true)
    public void assertGarageAcceptsJobCategoryOrUnrestricted(Garage garage, UUID jobCategoryId) {
        if (jobCategoryId == null) {
            return;
        }
        if (!garageCategoryAssignmentRepository.existsByGarage_Id(garage.getId())) {
            return;
        }
        if (!garageCategoryAssignmentRepository.existsByGarage_IdAndCategory_Id(garage.getId(), jobCategoryId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your garage profile is not enabled for this job category");
        }
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> getAcceptedCategoriesForGarageUser(String garageUserId) {
        Garage garage = garageRepository.findByUserId(garageUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));
        return garageCategoryAssignmentRepository.findByGarage_Id(garage.getId()).stream()
                .map(GarageCategoryAssignment::getCategory)
                .map(this::toCategoryResponse)
                .sorted(Comparator.comparing(ServiceCategoryResponse::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Transactional
    public void replaceAcceptedCategoriesForGarageUser(String garageUserId, Set<UUID> categoryIds) {
        Garage garage = garageRepository.findByUserId(garageUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found"));
        Set<UUID> unique = new LinkedHashSet<>(categoryIds);
        garageCategoryAssignmentRepository.deleteByGarage_Id(garage.getId());
        if (unique.isEmpty()) {
            return;
        }
        List<ServiceCategory> resolved = serviceCategoryRepository.findAllById(unique);
        if (resolved.size() != unique.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more category ids are invalid");
        }
        List<GarageCategoryAssignment> assignments = resolved.stream()
                .map(category -> new GarageCategoryAssignment(garage, category))
                .toList();
        garageCategoryAssignmentRepository.saveAll(assignments);
    }

    private ServiceCategoryResponse toCategoryResponse(ServiceCategory c) {
        return new ServiceCategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getIcon());
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
                .email(garage.getEmail())
                .dealership(garage.isDealership())
                .description(garage.getDescription())
                .services(garage.getServices())
                .isApproved(garage.getIsApproved())
                .averageRating(garage.getAverageRating())
                .totalReviews(garage.getTotalReviews())
                .build();
    }
}
