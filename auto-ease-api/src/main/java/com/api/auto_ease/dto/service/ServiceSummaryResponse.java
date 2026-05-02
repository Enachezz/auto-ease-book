package com.api.auto_ease.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSummaryResponse {

    private String uuid;
    private String name;
    private String description;
    private Integer phone;
    private String email;
    private String address;
    private Boolean dealership;
}
