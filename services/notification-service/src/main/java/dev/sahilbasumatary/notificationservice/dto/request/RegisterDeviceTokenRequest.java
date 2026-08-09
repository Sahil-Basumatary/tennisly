package dev.sahilbasumatary.notificationservice.dto.request;

import dev.sahilbasumatary.notificationservice.push.PushPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceTokenRequest(
        @NotBlank @Size(max = 512) String token, @NotNull PushPlatform platform) {}
