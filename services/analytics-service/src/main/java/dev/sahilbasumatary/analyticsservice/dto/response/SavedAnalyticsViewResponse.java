package dev.sahilbasumatary.analyticsservice.dto.response;

import dev.sahilbasumatary.analyticsservice.entity.SavedAnalyticsView;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SavedAnalyticsViewResponse(
        UUID id,
        String name,
        boolean favorite,
        Map<String, Object> config,
        int version,
        String organizationId,
        Instant createdAt,
        Instant updatedAt) {

    public static SavedAnalyticsViewResponse from(SavedAnalyticsView view) {
        return new SavedAnalyticsViewResponse(
                view.getId(),
                view.getName(),
                view.isFavorite(),
                view.getConfig(),
                view.getVersion(),
                view.getOrganizationId(),
                view.getCreatedAt(),
                view.getUpdatedAt());
    }
}
