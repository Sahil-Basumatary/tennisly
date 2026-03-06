package dev.sahilbasumatary.userservice.dto.request;

import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdatePreferencesRequest(
        @Size(max = 32) String theme,
        Boolean notificationsEnabled,
        Boolean emailNotifications,
        @Size(max = 64) String favoriteSurface,
        @Size(max = 16) String locale,
        Map<String, Object> extraSettings) {}
