package dev.sahilbasumatary.notificationservice.dto.response;

import dev.sahilbasumatary.notificationservice.push.DevicePushToken;
import dev.sahilbasumatary.notificationservice.push.PushPlatform;
import java.time.Instant;
import java.util.UUID;

public record DeviceTokenResponse(
        UUID id,
        PushPlatform platform,
        boolean active,
        String tokenSuffix,
        Instant lastSeenAt,
        Instant createdAt) {

    public static DeviceTokenResponse from(DevicePushToken token) {
        String raw = token.getToken();
        String suffix = raw == null || raw.length() < 8 ? "****" : "…" + raw.substring(raw.length() - 8);
        return new DeviceTokenResponse(
                token.getId(),
                token.getPlatform(),
                token.isActive(),
                suffix,
                token.getLastSeenAt(),
                token.getCreatedAt());
    }
}
