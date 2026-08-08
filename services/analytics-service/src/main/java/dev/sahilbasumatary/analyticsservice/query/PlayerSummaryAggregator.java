package dev.sahilbasumatary.analyticsservice.query;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import dev.sahilbasumatary.analyticsservice.dto.response.PlayerSummaryResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.SurfaceSummaryResponse;
import dev.sahilbasumatary.analyticsservice.index.PlayerMatchDocument;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PlayerSummaryAggregator {

    private static final int SUMMARY_SCAN_CAP = 10_000;

    private PlayerSummaryAggregator() {}

    public static int summaryScanCap() {
        return SUMMARY_SCAN_CAP;
    }

    public static PlayerSummaryResponse aggregate(List<PlayerMatchDocument> matches) {
        if (matches.isEmpty()) {
            return PlayerSummaryResponse.empty();
        }
        int wins = 0;
        int losses = 0;
        int pointsWon = 0;
        int servicePointsWon = 0;
        int breakPointsWon = 0;
        Map<String, MutableSurfaceSummary> bySurface = new HashMap<>();
        for (PlayerMatchDocument match : matches) {
            Boolean won = match.getWon();
            if (won != null) {
                if (won) {
                    wins++;
                } else {
                    losses++;
                }
            }
            TapeSideMetrics metrics = match.getMetrics();
            if (metrics != null) {
                pointsWon += metrics.pointsWon();
                servicePointsWon += metrics.servicePointsWon();
                breakPointsWon += metrics.breakPointsWon();
            }
            String surface = match.getSurface() == null ? "UNKNOWN" : match.getSurface();
            MutableSurfaceSummary surfaceSummary =
                    bySurface.computeIfAbsent(surface, ignored -> new MutableSurfaceSummary());
            surfaceSummary.matchesPlayed++;
            if (won != null) {
                if (won) {
                    surfaceSummary.wins++;
                } else {
                    surfaceSummary.losses++;
                }
            }
            if (metrics != null) {
                surfaceSummary.pointsWon += metrics.pointsWon();
                surfaceSummary.servicePointsWon += metrics.servicePointsWon();
                surfaceSummary.breakPointsWon += metrics.breakPointsWon();
            }
        }
        Map<String, SurfaceSummaryResponse> surfaceMap = new HashMap<>();
        bySurface.forEach(
                (surface, summary) ->
                        surfaceMap.put(
                                surface,
                                new SurfaceSummaryResponse(
                                        summary.matchesPlayed,
                                        summary.wins,
                                        summary.losses,
                                        summary.pointsWon,
                                        summary.servicePointsWon,
                                        summary.breakPointsWon)));
        return new PlayerSummaryResponse(
                matches.size(),
                wins,
                losses,
                pointsWon,
                servicePointsWon,
                breakPointsWon,
                Map.copyOf(surfaceMap));
    }

    private static final class MutableSurfaceSummary {
        private int matchesPlayed;
        private int wins;
        private int losses;
        private int pointsWon;
        private int servicePointsWon;
        private int breakPointsWon;
    }
}
