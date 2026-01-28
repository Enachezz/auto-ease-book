package com.api.auto_ease.repository.serviceSpecialization;

import com.api.auto_ease.domain.serviceSpecialization.ServiceSpecialization;
import com.api.auto_ease.domain.serviceSpecialization.ServiceSpecializationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceSpecializationRepository extends JpaRepository<ServiceSpecialization, ServiceSpecializationId> {
}


