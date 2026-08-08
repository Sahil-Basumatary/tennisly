package dev.sahilbasumatary.analyticsservice.dto.response;

import java.util.List;
import java.util.UUID;

public record PlayerAnalyticsResponse(
        UUID playerId,
        PlayerSummaryResponse summary,
        List<PlayerMatchRowResponse> matches,
        int page,
        int size,
        long totalMatches) {}
