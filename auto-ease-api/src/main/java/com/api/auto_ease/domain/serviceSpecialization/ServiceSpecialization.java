package com.api.auto_ease.domain.serviceSpecialization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "SERVICE_SPECIALIZATION")
@IdClass(ServiceSpecializationId.class)
public class ServiceSpecialization {

    @Id
    @Column(name = "service_uuid", length = 50)
    private String serviceUuid;

    @Id
    @Column(name = "name", length = 50)
    private String specializationName;

    @ManyToOne
    @JoinColumn(name = "service_uuid", insertable = false, updatable = false)
    private com.api.auto_ease.domain.service.Service service;

    @ManyToOne
    @JoinColumn(name = "name", insertable = false, updatable = false)
    private com.api.auto_ease.domain.specialization.Specialization specialization;
}


