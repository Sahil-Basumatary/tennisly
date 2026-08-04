package dev.sahilbasumatary.matchservice.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchPoint;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PointLedgerBuilderTest {

    @Test
    void competitiveRallyProducesOrderedLedgerWithSnapshots() {
        UUID home = BroadcastCatalogueIds.PLAYER_ALCARAZ;
        UUID away = BroadcastCatalogueIds.PLAYER_SINNER;
        Match match = new Match();
        List<MatchPoint> points =
                PointLedgerBuilder.build(
                        match, home, away, PointLedgerBuilder.competitiveRally(home, away, 12));
        assertEquals(12, points.size());
        assertEquals(1, points.get(0).getSequenceNumber());
        assertEquals(12, points.get(11).getSequenceNumber());
        assertFalse(points.get(0).getScoreSnapshot().isEmpty());
        assertTrue(match.getPoints().size() == 12);
        assertFalse(match.getCurrentScore().isEmpty());
    }
}
