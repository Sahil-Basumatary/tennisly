package dev.sahilbasumatary.analyticsservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CreateSavedViewRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Map<String, Object> config,
        Boolean favorite) {}
