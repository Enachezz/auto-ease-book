package com.api.auto_ease.dto.car;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCarRequest {

    @NotNull
    private UUID makeId;

    @NotNull
    private UUID modelId;

    @NotNull
    private Integer year;

    private String color;
    private String licensePlate;
    private String vin;
    private Integer mileage;
}
