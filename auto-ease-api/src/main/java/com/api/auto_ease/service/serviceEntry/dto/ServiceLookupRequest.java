package com.api.auto_ease.service.serviceEntry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceLookupRequest {

    private String serviceUuid;
    private Long entryId;
}


