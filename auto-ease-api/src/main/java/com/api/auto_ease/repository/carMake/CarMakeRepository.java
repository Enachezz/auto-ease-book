package com.api.auto_ease.repository.carMake;

import com.api.auto_ease.domain.carMake.CarMake;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CarMakeRepository extends JpaRepository<CarMake, UUID> {

    List<CarMake> findAllByOrderByNameAsc();
}
