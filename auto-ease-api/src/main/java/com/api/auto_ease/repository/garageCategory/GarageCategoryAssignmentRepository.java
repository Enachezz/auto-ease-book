package com.api.auto_ease.repository.garageCategory;

import com.api.auto_ease.domain.garageCategory.GarageCategoryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GarageCategoryAssignmentRepository extends JpaRepository<GarageCategoryAssignment, UUID> {

    List<GarageCategoryAssignment> findByGarage_Id(UUID garageId);

    boolean existsByGarage_Id(UUID garageId);

    boolean existsByGarage_IdAndCategory_Id(UUID garageId, UUID categoryId);

    void deleteByGarage_Id(UUID garageId);
}
