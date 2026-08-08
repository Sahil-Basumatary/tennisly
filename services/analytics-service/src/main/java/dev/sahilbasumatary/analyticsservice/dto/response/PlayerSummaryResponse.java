package dev.sahilbasumatary.analyticsservice.dto.response;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import java.util.Map;

public record PlayerSummaryResponse(
        int matchesPlayed,
        int wins,
        int losses,
        int pointsWon,
        int servicePointsWon,
        int breakPointsWon,
        Map<String, SurfaceSummaryResponse> bySurface) {

    public static PlayerSummaryResponse empty() {
        return new PlayerSummaryResponse(0, 0, 0, 0, 0, 0, Map.of());
    }
}
