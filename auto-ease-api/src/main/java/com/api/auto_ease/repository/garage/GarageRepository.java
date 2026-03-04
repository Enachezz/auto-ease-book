package com.api.auto_ease.repository.garage;

import com.api.auto_ease.domain.garage.Garage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GarageRepository extends JpaRepository<Garage, UUID> {

    Optional<Garage> findByUserId(String userId);

    boolean existsByUserId(String userId);

    List<Garage> findByIsApprovedTrue();
}
