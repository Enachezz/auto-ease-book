package com.api.auto_ease.repository.garageMake;

import com.api.auto_ease.domain.garageMake.GarageMakeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GarageMakeAssignmentRepository extends JpaRepository<GarageMakeAssignment, UUID> {

    List<GarageMakeAssignment> findByGarage_Id(UUID garageId);

    void deleteByGarage_Id(UUID garageId);
}
