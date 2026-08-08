package dev.sahilbasumatary.analyticsservice.dto.response;

import java.util.List;
import java.util.Map;

public record TournamentAnalyticsResponse(
        String tournamentKey,
        String tournamentName,
        Integer season,
        long matchCount,
        Map<String, Long> surfaceBreakdown,
        List<TournamentTopPlayerResponse> topPlayers) {}
