package dev.sahilbasumatary.analyticsservice.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import dev.sahilbasumatary.analyticsservice.index.MatchAnalyticsDocument;
import dev.sahilbasumatary.analyticsservice.index.PlayerMatchDocument;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalyticsCsvFormatterTest {

    @Test
    void formatMatchCsvIncludesHomeAndAwayRows() {
        UUID homeId = UUID.randomUUID();
        UUID awayId = UUID.randomUUID();
        MatchAnalyticsDocument match = new MatchAnalyticsDocument();
        match.setHomePlayerId(homeId);
        match.setHomeDisplayName("Home Player");
        match.setHomeMetrics(new TapeSideMetrics(20, 12, 3));
        match.setAwayPlayerId(awayId);
        match.setAwayDisplayName("Away, \"Pro\"");
        match.setAwayMetrics(new TapeSideMetrics(18, 10, 2));
        String csv = AnalyticsCsvFormatter.formatMatchCsv(match);
        assertTrue(csv.startsWith("side,playerId,displayName,pointsWon,servicePointsWon,breakPointsWon"));
        assertTrue(csv.contains("HOME," + homeId + ",Home Player,20,12,3"));
        assertTrue(csv.contains("AWAY," + awayId + ",\"Away, \"\"Pro\"\"\",18,10,2"));
    }

    @Test
    void formatPlayerMatchesCsvEscapesCommas() {
        PlayerMatchDocument row = new PlayerMatchDocument();
        row.setMatchId(UUID.randomUUID());
        row.setOpponentId(UUID.randomUUID());
        row.setOpponentName("Opponent, Jr.");
        row.setSurface("HARD");
        row.setEndedAt(Instant.parse("2024-07-01T12:00:00Z"));
        row.setScheduledAt(Instant.parse("2024-07-01T10:00:00Z"));
        row.setWon(true);
        row.setMetrics(new TapeSideMetrics(30, 18, 4));
        String csv = AnalyticsCsvFormatter.formatPlayerMatchesCsv(List.of(row));
        assertTrue(csv.contains("\"Opponent, Jr.\""));
        assertTrue(csv.contains(",30,18,4"));
    }

    @Test
    void csvCellQuotesSpecialCharacters() {
        assertEquals("", AnalyticsCsvFormatter.csvCell(null));
        assertEquals("plain", AnalyticsCsvFormatter.csvCell("plain"));
        assertEquals("\"a,b\"", AnalyticsCsvFormatter.csvCell("a,b"));
    }
}
