package com.api.auto_ease.repository.serviceCategory;

import com.api.auto_ease.domain.serviceCategory.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {

    List<ServiceCategory> findAllByOrderByNameAsc();
}
