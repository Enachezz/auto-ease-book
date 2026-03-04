package com.api.auto_ease.repository.carModel;

import com.api.auto_ease.domain.carModel.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CarModelRepository extends JpaRepository<CarModel, UUID> {

    List<CarModel> findByMakeIdOrderByNameAsc(UUID makeId);
}
