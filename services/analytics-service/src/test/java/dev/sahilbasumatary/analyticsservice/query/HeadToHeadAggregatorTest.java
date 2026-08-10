package dev.sahilbasumatary.analyticsservice.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import dev.sahilbasumatary.analyticsservice.dto.response.CompareResponse;
import dev.sahilbasumatary.analyticsservice.index.MatchAnalyticsDocument;
import dev.sahilbasumatary.analyticsservice.index.PlayerMatchDocument;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HeadToHeadAggregatorTest {

    @Test
    void aggregateCountsWinsLossesAndMetrics() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        UUID matchOne = UUID.randomUUID();
        UUID matchTwo = UUID.randomUUID();
        PlayerMatchDocument win = meeting(playerA, playerB, matchOne, true, new TapeSideMetrics(30, 20, 4));
        PlayerMatchDocument loss = meeting(playerA, playerB, matchTwo, false, new TapeSideMetrics(22, 14, 2));
        PlayerMatchDocument unknown = meeting(playerA, playerB, UUID.randomUUID(), null, new TapeSideMetrics(10, 6, 1));
        MatchAnalyticsDocument matchOneDoc =
                matchDoc(
                        matchOne,
                        playerA,
                        playerB,
                        new TapeSideMetrics(30, 20, 4),
                        new TapeSideMetrics(25, 15, 1));
        MatchAnalyticsDocument matchTwoDoc =
                matchDoc(
                        matchTwo,
                        playerA,
                        playerB,
                        new TapeSideMetrics(22, 14, 2),
                        new TapeSideMetrics(28, 18, 3));
        CompareResponse response =
                HeadToHeadAggregator.aggregate(
                        playerA,
                        playerB,
                        List.of(win, loss, unknown),
                        Map.of(matchOne, matchOneDoc, matchTwo, matchTwoDoc));
        assertEquals(3, response.meetingCount());
        assertEquals(1, response.aWins());
        assertEquals(1, response.bWins());
        assertEquals(1, response.unknownResults());
        assertEquals(62, response.playerA().pointsWon());
        assertEquals(53, response.playerB().pointsWon());
        assertEquals(3, response.meetings().size());
    }

    private static PlayerMatchDocument meeting(
            UUID playerA,
            UUID playerB,
            UUID matchId,
            Boolean won,
            TapeSideMetrics metrics) {
        PlayerMatchDocument document = new PlayerMatchDocument();
        document.setPlayerId(playerA);
        document.setOpponentId(playerB);
        document.setMatchId(matchId);
        document.setWon(won);
        document.setMetrics(metrics);
        document.setEndedAt(Instant.parse("2024-06-01T00:00:00Z"));
        return document;
    }

    private static MatchAnalyticsDocument matchDoc(
            UUID matchId,
            UUID homeId,
            UUID awayId,
            TapeSideMetrics homeMetrics,
            TapeSideMetrics awayMetrics) {
        MatchAnalyticsDocument document = new MatchAnalyticsDocument();
        document.setMatchId(matchId);
        document.setHomePlayerId(homeId);
        document.setAwayPlayerId(awayId);
        document.setHomeMetrics(homeMetrics);
        document.setAwayMetrics(awayMetrics);
        return document;
    }
}
