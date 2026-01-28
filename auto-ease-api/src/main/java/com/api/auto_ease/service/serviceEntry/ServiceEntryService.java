package com.api.auto_ease.service.serviceEntry;

import com.api.auto_ease.domain.appUser.AppUser;
import com.api.auto_ease.domain.appUser.AppUserType;
import com.api.auto_ease.domain.serviceEntry.ServiceEntry;
import com.api.auto_ease.repository.appUser.AppUserRepository;
import com.api.auto_ease.service.serviceEntry.dto.ClientServiceEntryRequest;
import com.api.auto_ease.service.serviceEntry.dto.ServiceLookupRequest;
import com.api.auto_ease.service.serviceEntry.strategy.ClientServiceEntryStrategy;
import com.api.auto_ease.service.serviceEntry.strategy.ServiceEntryStrategy;
import com.api.auto_ease.service.serviceEntry.strategy.ServiceServiceEntryStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ServiceEntryService {

    private final AppUserRepository appUserRepository;
    private final ClientServiceEntryStrategy clientStrategy;
    private final ServiceServiceEntryStrategy serviceStrategy;
    
    private Map<AppUserType, ServiceEntryStrategy> strategyMap;

    /**
     * Initialize strategy map after construction.
     * This method will be called by Spring after dependency injection.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        strategyMap = new HashMap<>();
        strategyMap.put(AppUserType.CLIENT, clientStrategy);
        strategyMap.put(AppUserType.SERVICE, serviceStrategy);
    }

    public ServiceEntry processServiceEntry(String userUuid, Object payload) {
        // Look up the user to determine their type
        AppUser user = appUserRepository.findById(userUuid)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userUuid));
        
        // Get the appropriate strategy based on user type
        ServiceEntryStrategy strategy = strategyMap.get(user.getType());
        
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for user type: " + user.getType());
        }

        if (user.getType() == AppUserType.CLIENT) {
            return clientStrategy.processServiceEntry((ClientServiceEntryRequest) payload);
        } else if (user.getType() == AppUserType.SERVICE) {
            return serviceStrategy.processServiceEntry((ServiceLookupRequest) payload);
        }

        throw new IllegalArgumentException("Unsupported user type: " + user.getType());
    }

    public ServiceEntry createServiceEntryForClient(ClientServiceEntryRequest request) {
        return clientStrategy.processServiceEntry(request);
    }

    public ServiceEntry lookupServiceEntryForService(ServiceLookupRequest lookupRequest) {
        return serviceStrategy.processServiceEntry(lookupRequest);
    }
}
