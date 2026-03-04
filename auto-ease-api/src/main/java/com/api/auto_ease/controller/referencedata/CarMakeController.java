package com.api.auto_ease.controller.referencedata;

import com.api.auto_ease.dto.referencedata.CarMakeResponse;
import com.api.auto_ease.dto.referencedata.CarModelResponse;
import com.api.auto_ease.repository.carMake.CarMakeRepository;
import com.api.auto_ease.repository.carModel.CarModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/car-makes")
@RequiredArgsConstructor
public class CarMakeController {

    private final CarMakeRepository carMakeRepository;
    private final CarModelRepository carModelRepository;

    @GetMapping
    public ResponseEntity<List<CarMakeResponse>> listMakes() {
        List<CarMakeResponse> makes = carMakeRepository.findAllByOrderByNameAsc().stream()
                .map(m -> new CarMakeResponse(m.getId(), m.getName()))
                .toList();
        return ResponseEntity.ok(makes);
    }

    @GetMapping("/{makeId}/models")
    public ResponseEntity<List<CarModelResponse>> listModels(@PathVariable UUID makeId) {
        List<CarModelResponse> models = carModelRepository.findByMakeIdOrderByNameAsc(makeId).stream()
                .map(m -> new CarModelResponse(m.getId(), m.getMakeId(), m.getName()))
                .toList();
        return ResponseEntity.ok(models);
    }
}
