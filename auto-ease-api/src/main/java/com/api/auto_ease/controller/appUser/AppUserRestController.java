package com.api.auto_ease.controller.appUser;

import com.api.auto_ease.domain.car.Car;
import com.api.auto_ease.service.appUser.AppUserService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppUserRestController {

    private AppUserService appUserService;

    @PutMapping(value = "api/v1/updateCar")
    public Car updateCar() {

        appUserService.updateCar();
    }
}
