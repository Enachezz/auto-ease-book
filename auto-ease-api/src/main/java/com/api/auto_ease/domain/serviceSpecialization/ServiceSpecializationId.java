package com.api.auto_ease.domain.serviceSpecialization;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ServiceSpecializationId implements Serializable {

    private String serviceUuid;
    private String specializationName;
}


