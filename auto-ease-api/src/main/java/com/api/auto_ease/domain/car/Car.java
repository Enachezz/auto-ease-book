package com.api.auto_ease.domain.car;

import com.api.auto_ease.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "CAR")
public class Car extends BaseEntity {

    @Column(name = "make")
    private String make;

    @Column(name = "model")
    private String model;

    @Column(name = "made")
    private LocalDate made;

    @Column(name = "color")
    private String color;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "current_mileage")
    private Integer currentMileage;

    @Column(name = "vin")
    private String vin;
}
