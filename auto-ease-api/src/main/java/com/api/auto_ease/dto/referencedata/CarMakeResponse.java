package com.api.auto_ease.dto.referencedata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarMakeResponse {

    private UUID id;
    private String name;
}
