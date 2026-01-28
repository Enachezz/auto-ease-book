package com.api.auto_ease.repository.serviceEntry;

import com.api.auto_ease.domain.serviceEntry.ServiceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceEntryRepository extends JpaRepository<ServiceEntry, Long> {

    List<ServiceEntry> findAlLByPriority(int priority);

    List<ServiceEntry> findAllOrderByPriorityDesc();

    List<ServiceEntry> findAllOrderByPrioryAsc();

    List<ServiceEntry> findByServiceUuid(String serviceUuid);

    List<ServiceEntry> findByClientUuid(String clientUuid);

    Optional<ServiceEntry> findByClientUuidAndServiceUuid(String clientUuid, String serviceUuid);
}
