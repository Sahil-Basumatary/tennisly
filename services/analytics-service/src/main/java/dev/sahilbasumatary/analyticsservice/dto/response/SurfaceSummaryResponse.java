package dev.sahilbasumatary.analyticsservice.dto.response;

public record SurfaceSummaryResponse(
        int matchesPlayed,
        int wins,
        int losses,
        int pointsWon,
        int servicePointsWon,
        int breakPointsWon) {}
