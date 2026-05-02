package com.api.auto_ease.dto.garage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedGaragesResponse {

    private List<GarageResponse> garages;
    private long totalCount;
    private int page;
    private int pageSize;
    private int totalPages;
}
