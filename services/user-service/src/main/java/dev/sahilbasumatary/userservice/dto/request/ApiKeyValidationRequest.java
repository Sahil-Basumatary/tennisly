package dev.sahilbasumatary.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ApiKeyValidationRequest(@NotBlank String apiKey) {}
