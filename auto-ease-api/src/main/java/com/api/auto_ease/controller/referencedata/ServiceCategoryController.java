package com.api.auto_ease.controller.referencedata;

import com.api.auto_ease.dto.referencedata.ServiceCategoryResponse;
import com.api.auto_ease.repository.serviceCategory.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCategoryRepository serviceCategoryRepository;

    @GetMapping("/api/service-categories")
    public List<ServiceCategoryResponse> listCategories() {
        return serviceCategoryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new ServiceCategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getIcon()))
                .toList();
    }
}
