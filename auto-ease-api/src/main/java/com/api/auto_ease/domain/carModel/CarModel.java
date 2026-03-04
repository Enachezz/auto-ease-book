package com.api.auto_ease.domain.carModel;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.LocalDateTime.now;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "car_models")
public class CarModel {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @Column(name = "make_id", nullable = false)
    private UUID makeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        createdDate = now();
    }
}
