package com.api.auto_ease.repository.serviceEntry;

import com.api.auto_ease.domain.serviceEntry.ServiceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceEntryRepository extends JpaRepository<ServiceEntry, Long> {

    List<ServiceEntry> findAlLByPriority(int priority);

    List<ServiceEntry> findAllByOrderByPriorityDesc();

    List<ServiceEntry> findAllByOrderByPriorityAsc();

    List<ServiceEntry> findByGarageId(UUID garageId);

    List<ServiceEntry> findByClientUuid(String clientUuid);

    List<ServiceEntry> findAllByCarVin(String vinNumber);

    Optional<ServiceEntry> findByClientUuidAndGarageId(String clientUuid, UUID garageId);
}
