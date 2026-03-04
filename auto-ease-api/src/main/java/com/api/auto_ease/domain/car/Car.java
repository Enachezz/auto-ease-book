package com.api.auto_ease.domain.car;

import com.api.auto_ease.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "CAR")
public class Car extends BaseEntity {

    @Column(name = "user_id")
    private String userId;

    @Column(name = "make_id")
    private UUID makeId;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "year")
    private Integer year;

    @Column(name = "color")
    private String color;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "vin")
    private String vin;

    @Column(name = "mileage")
    private Integer mileage;
}
