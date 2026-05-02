package com.api.auto_ease.controller.service;

import com.api.auto_ease.domain.serviceEntry.ServiceEntry;
import com.api.auto_ease.dto.service.PagedServicesResponse;
import com.api.auto_ease.dto.service.ServiceSearchRequest;
import com.api.auto_ease.dto.service.ServiceSummaryResponse;
import com.api.auto_ease.service.service.ServiceEntityService;
import com.api.auto_ease.service.serviceEntry.ServiceEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ServiceRestController {

    private final ServiceEntryService serviceEntryService;
    private final ServiceEntityService serviceEntityService;

    @GetMapping(value = "/api/v1/service/{id}")
    public ServiceSummaryResponse getServiceById(@PathVariable String id) {
        return serviceEntityService.getServiceById(id);
    }

    @PostMapping("/api/v1/services/search")
    public PagedServicesResponse searchServices(@Valid @RequestBody(required = false) ServiceSearchRequest request) {
        ServiceSearchRequest body = request != null ? request : new ServiceSearchRequest();
        return serviceEntityService.searchServices(body);
    }

    //entry point for service entry request for users and service entry retrieval for mechanics
    @PostMapping(value = "/api/v1/processServiceEntry")
    public ServiceEntry processServiceEntry(String uuid, Object payload) {
        return serviceEntryService.processServiceEntry(uuid, payload);
    }
}
