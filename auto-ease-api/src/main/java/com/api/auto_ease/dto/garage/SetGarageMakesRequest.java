package com.api.auto_ease.dto.garage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class SetGarageMakesRequest {

    @NotNull
    @Size(max = 5)
    @Builder.Default
    private Set<UUID> makeIds = new LinkedHashSet<>();
}
