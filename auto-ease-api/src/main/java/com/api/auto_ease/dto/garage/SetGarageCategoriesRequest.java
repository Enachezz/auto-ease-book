package com.api.auto_ease.dto.garage;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetGarageCategoriesRequest {

    @NotNull
    @Builder.Default
    private Set<UUID> categoryIds = new LinkedHashSet<>();
}
