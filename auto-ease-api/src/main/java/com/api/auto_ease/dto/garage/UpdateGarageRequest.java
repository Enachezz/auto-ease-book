package com.api.auto_ease.dto.garage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGarageRequest {

    private String businessName;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String phone;
    private String description;
    private String[] services;
}
