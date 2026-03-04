package com.api.auto_ease.repository.car;

import com.api.auto_ease.domain.car.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Integer> {

    List<Car> findByUserIdOrderByCreatedDateDesc(String userId);
}
