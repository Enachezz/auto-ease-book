package com.api.auto_ease.dto.car;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCarRequest {

    private UUID makeId;
    private UUID modelId;
    private Integer year;
    private String color;
    private String licensePlate;
    private String vin;
    private Integer mileage;
}
