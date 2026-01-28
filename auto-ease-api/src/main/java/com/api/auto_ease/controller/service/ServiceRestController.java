package com.api.auto_ease.controller.service;

import com.api.auto_ease.domain.serviceEntry.ServiceEntry;
import com.api.auto_ease.service.serviceEntry.ServiceEntryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceRestController {

    private ServiceEntryService serviceEntryService;


    //entry point for service entry request for users and service entry retrieval for mechanics
    @PostMapping(value = "/api/v1/processServiceEntry")
    public ServiceEntry processServiceEntry(String uuid, Object payload) {
        return serviceEntryService.processServiceEntry(uuid, payload);
    }
}
