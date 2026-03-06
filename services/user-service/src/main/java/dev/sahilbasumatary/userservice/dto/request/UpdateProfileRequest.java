package dev.sahilbasumatary.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 255) String displayName,
        @Size(max = 255) String firstName,
        @Size(max = 255) String lastName,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String phone,
        @Size(max = 100) String country,
        @Size(max = 100) String timezone,
        @Size(max = 2000) String bio,
        @Size(max = 512) String avatarUrl,
        @Size(max = 32) String skillLevel) {}
