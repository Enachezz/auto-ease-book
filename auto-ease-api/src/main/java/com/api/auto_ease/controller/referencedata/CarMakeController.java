package com.api.auto_ease.controller.referencedata;

import com.api.auto_ease.dto.referencedata.CarMakeResponse;
import com.api.auto_ease.dto.referencedata.CarModelResponse;
import com.api.auto_ease.repository.carMake.CarMakeRepository;
import com.api.auto_ease.repository.carModel.CarModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CarMakeController {

    private final CarMakeRepository carMakeRepository;
    private final CarModelRepository carModelRepository;

    @GetMapping("/api/car-makes")
    public List<CarMakeResponse> listMakes() {
        return carMakeRepository.findAllByOrderByNameAsc().stream()
                .map(m -> new CarMakeResponse(m.getId(), m.getName()))
                .toList();
    }

    @GetMapping("/api/car-makes/{makeId}/models")
    public List<CarModelResponse> listModels(@PathVariable UUID makeId) {
        return carModelRepository.findByMakeIdOrderByNameAsc(makeId).stream()
                .map(m -> new CarModelResponse(m.getId(), m.getMakeId(), m.getName()))
                .toList();
    }
}
