package com.api.auto_ease.service.serviceEntry.strategy;

import com.api.auto_ease.domain.appUser.AppUserType;
import com.api.auto_ease.domain.serviceEntry.ServiceEntry;
import com.api.auto_ease.repository.serviceEntry.ServiceEntryRepository;
import com.api.auto_ease.service.serviceEntry.dto.ClientServiceEntryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientServiceEntryStrategy implements ServiceEntryStrategy<ClientServiceEntryRequest> {

    private final ServiceEntryRepository serviceEntryRepository;

    @Override
    public ServiceEntry processServiceEntry(ClientServiceEntryRequest payload) {
        ServiceEntry serviceEntry = new ServiceEntry();

        if (payload != null) {
            serviceEntry.setClientUuid(payload.getClientUuid());
            serviceEntry.setServiceUuid(payload.getServiceUuid());
            serviceEntry.setEntryType(payload.getEntryType());
            serviceEntry.setCarMake(payload.getCarMake());
            serviceEntry.setCarModel(payload.getCarModel());
            serviceEntry.setPriority(payload.getPriority());
            if (payload.getCarYear() != null) {
                serviceEntry.setCarYear(payload.getCarYear());
            }
            serviceEntry.setCarVin(payload.getCarVin());
            serviceEntry.setDescription(payload.getDescription());
            serviceEntry.setServiceDate(payload.getServiceDate());
            serviceEntry.setLocation(payload.getLocation());
            if (payload.getPriority() != null) {
                serviceEntry.setPriority(payload.getPriority());
            }
            serviceEntry.setCost(payload.getCost());
            serviceEntry.setNote(payload.getNote());
        }

        return serviceEntryRepository.save(serviceEntry);
    }

    @Override
    public AppUserType getUserType() {
        return AppUserType.CLIENT;
    }
}

