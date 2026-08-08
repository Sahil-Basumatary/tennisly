package dev.sahilbasumatary.analyticsservice.query;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import dev.sahilbasumatary.analyticsservice.index.MatchAnalyticsDocument;
import dev.sahilbasumatary.analyticsservice.index.PlayerMatchDocument;
import java.util.List;

public final class AnalyticsCsvFormatter {

    private static final String MATCH_HEADER =
            "side,playerId,displayName,pointsWon,servicePointsWon,breakPointsWon";

    private static final String PLAYER_HEADER =
            "matchId,opponentId,opponentName,surface,endedAt,scheduledAt,won,pointsWon,servicePointsWon,breakPointsWon";

    private AnalyticsCsvFormatter() {}

    public static String formatMatchCsv(MatchAnalyticsDocument match) {
        StringBuilder csv = new StringBuilder(MATCH_HEADER).append('\n');
        appendMatchSide(csv, "HOME", match.getHomePlayerId(), match.getHomeDisplayName(), match.getHomeMetrics());
        appendMatchSide(csv, "AWAY", match.getAwayPlayerId(), match.getAwayDisplayName(), match.getAwayMetrics());
        return csv.toString();
    }

    public static String formatPlayerMatchesCsv(List<PlayerMatchDocument> matches) {
        StringBuilder csv = new StringBuilder(PLAYER_HEADER).append('\n');
        for (PlayerMatchDocument match : matches) {
            TapeSideMetrics metrics = match.getMetrics() == null
                    ? new TapeSideMetrics(0, 0, 0)
                    : match.getMetrics();
            csv.append(csvCell(match.getMatchId()))
                    .append(',')
                    .append(csvCell(match.getOpponentId()))
                    .append(',')
                    .append(csvCell(match.getOpponentName()))
                    .append(',')
                    .append(csvCell(match.getSurface()))
                    .append(',')
                    .append(csvCell(match.getEndedAt()))
                    .append(',')
                    .append(csvCell(match.getScheduledAt()))
                    .append(',')
                    .append(csvCell(match.getWon()))
                    .append(',')
                    .append(metrics.pointsWon())
                    .append(',')
                    .append(metrics.servicePointsWon())
                    .append(',')
                    .append(metrics.breakPointsWon())
                    .append('\n');
        }
        return csv.toString();
    }

    private static void appendMatchSide(
            StringBuilder csv,
            String side,
            Object playerId,
            String displayName,
            TapeSideMetrics metrics) {
        TapeSideMetrics safe = metrics == null ? new TapeSideMetrics(0, 0, 0) : metrics;
        csv.append(side)
                .append(',')
                .append(csvCell(playerId))
                .append(',')
                .append(csvCell(displayName))
                .append(',')
                .append(safe.pointsWon())
                .append(',')
                .append(safe.servicePointsWon())
                .append(',')
                .append(safe.breakPointsWon())
                .append('\n');
    }

    static String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String raw = String.valueOf(value);
        if (raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r")) {
            return "\"" + raw.replace("\"", "\"\"") + "\"";
        }
        return raw;
    }
}
