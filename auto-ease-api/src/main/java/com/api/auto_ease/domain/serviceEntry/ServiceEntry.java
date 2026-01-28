package com.api.auto_ease.domain.serviceEntry;

import com.api.auto_ease.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "SERVICE_ENTRY")
public class ServiceEntry extends BaseEntity {

    @Column(name = "client_uuid")
    private String clientUuid;

    @Column(name = "service_uuid")
    private String serviceUuid;

    @Column(name = "entry_type")
    private String entryType;

    @Column(name = "car_make")
    private String carMake;

    @Column(name = "car_model")
    private String carModel;

    @Column(name = "car_year")
    private int carYear;

    @Column(name = "car_vin")
    private String carVin;

    @Column(name = "description")
    private String description;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @Column(name = "location")
    private String location;

    @Column(name = "priority")
    private int priority;

    @Column(name = "cost")
    private BigDecimal cost;

    @Column(name = "note")
    private String note;
}
