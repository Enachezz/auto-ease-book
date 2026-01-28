package com.api.auto_ease.service.appUser;

import com.api.auto_ease.domain.car.Car;
import com.api.auto_ease.repository.car.CarRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AppUserService {

    private CarRepository carRepository;

    public Car updateCar() {

        return null;
    }
}
