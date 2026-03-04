package com.api.auto_ease.dto.garage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GarageResponse {

    private UUID id;
    private String userId;
    private String businessName;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String phone;
    private String description;
    private String[] services;
    private Boolean isApproved;
    private BigDecimal averageRating;
    private Integer totalReviews;
}
