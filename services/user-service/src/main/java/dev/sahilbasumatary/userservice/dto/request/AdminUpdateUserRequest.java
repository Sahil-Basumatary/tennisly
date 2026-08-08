package dev.sahilbasumatary.userservice.dto.request;

import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequest(
        @Size(max = 255) String displayName,
        Boolean active) {}
