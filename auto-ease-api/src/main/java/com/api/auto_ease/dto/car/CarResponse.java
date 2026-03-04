package com.api.auto_ease.dto.car;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarResponse {

    private Integer id;
    private String makeId;
    private String makeName;
    private String modelId;
    private String modelName;
    private Integer year;
    private String color;
    private String licensePlate;
    private String vin;
    private Integer mileage;
}
