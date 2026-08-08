package dev.sahilbasumatary.analyticsservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sahilbasumatary.analyticsservice.client.dto.MatchPlayerSummary;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchPointSummary;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchSummary;
import dev.sahilbasumatary.analyticsservice.domain.TapeMatchMetrics;
import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TapeMetricAggregatorTest {

    private static final UUID HOME_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AWAY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MATCH_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private TapeMetricAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new TapeMetricAggregator();
    }

    @Test
    void aggregatesPointsWonBySide() {
        MatchSummary match = match(List.of(home(), away()));
        List<MatchPointSummary> points =
                List.of(
                        point(1, HOME_ID, HOME_ID, Map.of("points", List.of("15", "0"))),
                        point(2, HOME_ID, AWAY_ID, Map.of("points", List.of("15", "15"))),
                        point(3, AWAY_ID, AWAY_ID, Map.of("points", List.of("0", "30"))));
        TapeMatchMetrics metrics = aggregator.aggregate(match, points);
        assertEquals(new TapeSideMetrics(1, 1, 0), metrics.home());
        assertEquals(new TapeSideMetrics(2, 1, 0), metrics.away());
        assertEquals(3, metrics.pointsPlayed());
    }

    @Test
    void countsServicePointsWon() {
        MatchSummary match = match(List.of(home(), away()));
        List<MatchPointSummary> points =
                List.of(
                        point(1, HOME_ID, HOME_ID, Map.of("points", List.of("15", "0"))),
                        point(2, HOME_ID, AWAY_ID, Map.of("points", List.of("15", "15"))),
                        point(3, AWAY_ID, AWAY_ID, Map.of("points", List.of("0", "30"))));
        TapeMatchMetrics metrics = aggregator.aggregate(match, points);
        assertEquals(1, metrics.home().servicePointsWon());
        assertEquals(1, metrics.away().servicePointsWon());
    }

    @Test
    void countsBreakPointsAtLoveLove() {
        MatchSummary match = match(List.of(home(), away()));
        List<MatchPointSummary> points =
                List.of(
                        point(1, HOME_ID, AWAY_ID, Map.of("points", List.of("0", "0"))),
                        point(2, AWAY_ID, HOME_ID, Map.of("points", List.of("0", "0"))),
                        point(3, HOME_ID, HOME_ID, Map.of("points", List.of("15", "0"))));
        TapeMatchMetrics metrics = aggregator.aggregate(match, points);
        assertEquals(new TapeSideMetrics(2, 1, 1), metrics.home());
        assertEquals(new TapeSideMetrics(1, 0, 1), metrics.away());
    }

    @Test
    void ignoresBreakWhenServerWins() {
        MatchSummary match = match(List.of(home(), away()));
        List<MatchPointSummary> points =
                List.of(point(1, HOME_ID, HOME_ID, Map.of("points", List.of("0", "0"))));
        TapeMatchMetrics metrics = aggregator.aggregate(match, points);
        assertEquals(new TapeSideMetrics(1, 1, 0), metrics.home());
        assertEquals(new TapeSideMetrics(0, 0, 0), metrics.away());
    }

    @Test
    void ignoresBreakWhenScoreSnapshotMissingPointsArray() {
        MatchSummary match = match(List.of(home(), away()));
        List<MatchPointSummary> points =
                List.of(
                        point(1, HOME_ID, AWAY_ID, Map.of("game", Map.of("HOME", "0", "AWAY", "0"))),
                        point(2, HOME_ID, AWAY_ID, Map.of("points", List.of("15"))));
        TapeMatchMetrics metrics = aggregator.aggregate(match, points);
        assertEquals(new TapeSideMetrics(0, 0, 0), metrics.home());
        assertEquals(new TapeSideMetrics(2, 0, 0), metrics.away());
    }

    @Test
    void emptyPointsYieldZeroMetrics() {
        MatchSummary match = match(List.of(home(), away()));
        TapeMatchMetrics metrics = aggregator.aggregate(match, List.of());
        assertEquals(new TapeSideMetrics(0, 0, 0), metrics.home());
        assertEquals(new TapeSideMetrics(0, 0, 0), metrics.away());
        assertEquals(0, metrics.pointsPlayed());
    }

    @Test
    void throwsWhenHomePlayerMissing() {
        MatchSummary match = match(List.of(away()));
        assertThrows(
                IllegalArgumentException.class,
                () -> aggregator.aggregate(match, List.of()));
    }

    @Test
    void throwsWhenAwayPlayerMissing() {
        MatchSummary match = match(List.of(home()));
        assertThrows(
                IllegalArgumentException.class,
                () -> aggregator.aggregate(match, List.of()));
    }

    private static MatchPlayerSummary home() {
        return new MatchPlayerSummary(HOME_ID, "Home Player", "HOME");
    }

    private static MatchPlayerSummary away() {
        return new MatchPlayerSummary(AWAY_ID, "Away Player", "AWAY");
    }

    private static MatchSummary match(List<MatchPlayerSummary> players) {
        return new MatchSummary(
                MATCH_ID,
                "ext-1",
                UUID.randomUUID(),
                "HARD",
                "COMPLETED",
                3,
                Instant.parse("2026-01-15T12:00:00Z"),
                Instant.parse("2026-01-15T12:05:00Z"),
                Instant.parse("2026-01-15T14:00:00Z"),
                Map.of("provider", "livetennis", "tournamentName", "Australian Open", "season", 2026),
                Map.of(),
                players,
                0);
    }

    private static MatchPointSummary point(
            int sequence, UUID serverId, UUID winnerId, Map<String, Object> scoreSnapshot) {
        return new MatchPointSummary(
                UUID.randomUUID(),
                sequence,
                serverId,
                winnerId,
                "WINNER",
                3,
                scoreSnapshot);
    }
}
