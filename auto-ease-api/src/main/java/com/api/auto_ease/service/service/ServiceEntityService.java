package com.api.auto_ease.service.service;

import com.api.auto_ease.domain.service.ServiceEntity;
import com.api.auto_ease.dto.service.PagedServicesResponse;
import com.api.auto_ease.dto.service.ServiceFilterCriteria;
import com.api.auto_ease.dto.service.ServiceSearchRequest;
import com.api.auto_ease.dto.service.ServiceSummaryResponse;
import com.api.auto_ease.repository.service.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
public class ServiceEntityService {

    public static final int SERVICES_PAGE_SIZE = 20;

    private final ServiceRepository serviceRepository;

    public PagedServicesResponse searchServices(ServiceSearchRequest request) {
        ServiceFilterCriteria filter = request.getFilter() != null
                ? request.getFilter()
                : ServiceFilterCriteria.builder().build();
        var pageable = PageRequest.of(request.getPage(), SERVICES_PAGE_SIZE, Sort.by("name").ascending());
        Page<ServiceEntity> page = serviceRepository.findAll(ServiceSpecifications.fromFilter(filter), pageable);
        return PagedServicesResponse.builder()
                .services(page.getContent().stream().map(this::toSummary).toList())
                .totalCount(page.getTotalElements())
                .page(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    public ServiceSummaryResponse getServiceById(String uuid) {
        return serviceRepository.findById(uuid)
                .map(this::toSummary)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
    }

    private ServiceSummaryResponse toSummary(ServiceEntity entity) {
        return ServiceSummaryResponse.builder()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .description(entity.getDescription())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .dealership(entity.isDealership())
                .build();
    }
}
