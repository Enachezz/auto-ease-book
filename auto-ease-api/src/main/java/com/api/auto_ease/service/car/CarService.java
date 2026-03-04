package com.api.auto_ease.service.car;

import com.api.auto_ease.domain.car.Car;
import com.api.auto_ease.domain.carMake.CarMake;
import com.api.auto_ease.domain.carModel.CarModel;
import com.api.auto_ease.dto.car.CarResponse;
import com.api.auto_ease.dto.car.CreateCarRequest;
import com.api.auto_ease.dto.car.UpdateCarRequest;
import com.api.auto_ease.repository.car.CarRepository;
import com.api.auto_ease.repository.carMake.CarMakeRepository;
import com.api.auto_ease.repository.carModel.CarModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final CarMakeRepository carMakeRepository;
    private final CarModelRepository carModelRepository;

    @Transactional
    public CarResponse addCar(String userId, CreateCarRequest request) {
        CarMake make = carMakeRepository.findById(request.getMakeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid make ID"));

        CarModel model = carModelRepository.findById(request.getModelId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid model ID"));

        if (!model.getMakeId().equals(request.getMakeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model does not belong to the specified make");
        }

        Car car = new Car(userId, request.getMakeId(), request.getModelId(),
                request.getYear(), request.getColor(), request.getLicensePlate(),
                request.getVin(), request.getMileage());

        car = carRepository.save(car);

        return toResponse(car, make.getName(), model.getName());
    }

    public List<CarResponse> getMyCars(String userId) {
        return carRepository.findByUserIdOrderByCreatedDateDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CarResponse updateCar(String userId, Integer carId, UpdateCarRequest request) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found"));

        if (!car.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this car");
        }

        if (request.getMakeId() != null) {
            carMakeRepository.findById(request.getMakeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid make ID"));
            car.setMakeId(request.getMakeId());
        }

        if (request.getModelId() != null) {
            carModelRepository.findById(request.getModelId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid model ID"));
            car.setModelId(request.getModelId());
        }

        if (request.getYear() != null) car.setYear(request.getYear());
        if (request.getColor() != null) car.setColor(request.getColor());
        if (request.getLicensePlate() != null) car.setLicensePlate(request.getLicensePlate());
        if (request.getVin() != null) car.setVin(request.getVin());
        if (request.getMileage() != null) car.setMileage(request.getMileage());

        car = carRepository.save(car);

        return toResponse(car);
    }

    @Transactional
    public void deleteCar(String userId, Integer carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found"));

        if (!car.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this car");
        }

        carRepository.delete(car);
    }

    private CarResponse toResponse(Car car) {
        String makeName = carMakeRepository.findById(car.getMakeId())
                .map(CarMake::getName).orElse("Unknown");
        String modelName = carModelRepository.findById(car.getModelId())
                .map(CarModel::getName).orElse("Unknown");
        return toResponse(car, makeName, modelName);
    }

    private CarResponse toResponse(Car car, String makeName, String modelName) {
        return CarResponse.builder()
                .id(car.getId())
                .makeName(makeName)
                .modelName(modelName)
                .year(car.getYear())
                .color(car.getColor())
                .licensePlate(car.getLicensePlate())
                .vin(car.getVin())
                .mileage(car.getMileage())
                .build();
    }
}
