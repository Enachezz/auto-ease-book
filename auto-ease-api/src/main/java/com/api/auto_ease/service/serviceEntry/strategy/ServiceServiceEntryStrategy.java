package com.api.auto_ease.service.serviceEntry.strategy;

import com.api.auto_ease.domain.appUser.AppUserType;
import com.api.auto_ease.domain.serviceEntry.ServiceEntry;
import com.api.auto_ease.exception.MessageException;
import com.api.auto_ease.repository.serviceEntry.ServiceEntryRepository;
import com.api.auto_ease.service.serviceEntry.dto.ServiceLookupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Strategy for SERVICE users.
 * Looks up existing service entries that match the service provider.
 */
@Component
@RequiredArgsConstructor
public class ServiceServiceEntryStrategy implements ServiceEntryStrategy<ServiceLookupRequest> {

    private final ServiceEntryRepository serviceEntryRepository;

    @Override
    public ServiceEntry processServiceEntry(ServiceLookupRequest payload) {
        // For service providers, we look up existing service entries
        ServiceEntry foundEntry = null;

        if (payload != null && payload.getEntryId() != null) {
            Optional<ServiceEntry> entry = serviceEntryRepository.findById(payload.getEntryId());
            if (entry.isPresent() && payload.getServiceUuid() != null 
                && entry.get().getServiceUuid() == null) {
                foundEntry = entry.get();
            }
        }

        // If no specific entry found, get the first available entry for this service
        if (foundEntry == null) {
            throw new MessageException("Something went wrong");
        }

        foundEntry.setServiceUuid(payload.getServiceUuid());

        return foundEntry;
    }

    @Override
    public AppUserType getUserType() {
        return AppUserType.SERVICE;
    }
}

