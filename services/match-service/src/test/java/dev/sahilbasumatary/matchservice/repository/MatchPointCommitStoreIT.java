package dev.sahilbasumatary.matchservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.matchservice.dto.request.RecordPointRequest;
import dev.sahilbasumatary.matchservice.entity.PointOutcome;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class MatchPointCommitStoreIT {

    private static JdbcTemplate jdbc;
    private static MatchPointCommitStore store;
    private static TransactionTemplate transactions;

    @BeforeAll
    static void start() {
        DataSource dataSource;
        try {
            dataSource = PostgresMatchHarness.dataSource();
        } catch (RuntimeException ex) {
            Assumptions.assumeTrue(false, ex.getMessage());
            return;
        }
        jdbc = new JdbcTemplate(dataSource);
        store = new MatchPointCommitStore(jdbc, new ObjectMapper().findAndRegisterModules());
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void commitsPointMatchEventAndOutboxTogether() {
        SeededMatch seeded = seedMatch();
        RecordPointRequest request =
                new RecordPointRequest(
                        seeded.home,
                        seeded.away,
                        PointOutcome.WINNER,
                        3,
                        Map.of("game", "15-0"),
                        Map.of());
        MatchEvent event =
                MatchEvent.pointRecorded(seeded.matchId, "IN_PROGRESS", 1, seeded.away, "WINNER");
        CommittedPoint committed =
                transactions.execute(status -> store.commit(seeded.snapshot(), request, event));
        assertEquals(1, committed.sequenceNumber());
        assertEquals(1L, committed.liveSequence());
        assertEquals(
                1L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM match_points WHERE match_id = ?",
                        Long.class,
                        seeded.matchId));
        assertEquals(
                1L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM match_event_logs WHERE match_id = ? AND event_type = 'POINT_RECORDED'",
                        Long.class,
                        seeded.matchId));
        assertEquals(
                1L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM match_outbox WHERE event_json->>'matchId' = ?",
                        Long.class,
                        seeded.matchId.toString()));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT point_count FROM matches WHERE id = ?",
                        Integer.class,
                        seeded.matchId));
        assertEquals(Integer.valueOf(1), event.getPointSequence());
        assertEquals(
                "1",
                jdbc.queryForObject(
                        "SELECT event_json->>'pointSequence' FROM match_outbox WHERE event_json->>'matchId' = ?",
                        String.class,
                        seeded.matchId.toString()));
    }

    @Test
    void overwritesStalePointSequenceWithTheDatabaseReturningValue() {
        SeededMatch seeded = seedMatch();
        RecordPointRequest first =
                new RecordPointRequest(
                        seeded.home,
                        seeded.away,
                        PointOutcome.WINNER,
                        2,
                        Map.of("game", "15-0"),
                        Map.of());
        RecordPointRequest second =
                new RecordPointRequest(
                        seeded.home,
                        seeded.away,
                        PointOutcome.ACE,
                        1,
                        Map.of("game", "30-0"),
                        Map.of());
        MatchEvent firstEvent =
                MatchEvent.pointRecorded(seeded.matchId, "IN_PROGRESS", null, seeded.away, "WINNER");
        MatchEvent secondEvent =
                MatchEvent.pointRecorded(seeded.matchId, "IN_PROGRESS", 1, seeded.home, "ACE");
        transactions.execute(status -> store.commit(seeded.snapshot(), first, firstEvent));
        CommittedPoint committed =
                transactions.execute(status -> store.commit(seeded.snapshot(), second, secondEvent));
        assertEquals(2, committed.sequenceNumber());
        assertEquals(Integer.valueOf(2), secondEvent.getPointSequence());
        assertEquals(
                1L,
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM match_outbox
                        WHERE event_json->>'matchId' = ?
                          AND event_json->>'pointSequence' = '2'
                        """,
                        Long.class,
                        seeded.matchId.toString()));
    }

    @Test
    void rollsAllFourWritesBackWhenTheTransactionFails() {
        SeededMatch seeded = seedMatch();
        RecordPointRequest request =
                new RecordPointRequest(
                        seeded.home,
                        seeded.away,
                        PointOutcome.ACE,
                        1,
                        Map.of("game", "15-0"),
                        Map.of());
        MatchEvent event =
                MatchEvent.pointRecorded(seeded.matchId, "IN_PROGRESS", 1, seeded.home, "ACE");
        assertThrows(
                IllegalStateException.class,
                () ->
                        transactions.executeWithoutResult(
                                status -> {
                                    store.commit(seeded.snapshot(), request, event);
                                    throw new IllegalStateException("forced rollback");
                                }));
        assertEquals(
                0L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM match_points WHERE match_id = ?",
                        Long.class,
                        seeded.matchId));
        assertEquals(
                0L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM match_event_logs WHERE match_id = ?",
                        Long.class,
                        seeded.matchId));
        assertEquals(
                0L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM match_outbox WHERE event_json->>'matchId' = ?",
                        Long.class,
                        seeded.matchId.toString()));
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT point_count FROM matches WHERE id = ?",
                        Integer.class,
                        seeded.matchId));
    }

    private SeededMatch seedMatch() {
        UUID matchId = UUID.randomUUID();
        UUID home = UUID.randomUUID();
        UUID away = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO matches (
                    id, external_id, surface, status, best_of_sets,
                    current_score, metadata, point_count, live_sequence
                )
                VALUES (?, ?, 'HARD', 'IN_PROGRESS', 3, '{}'::jsonb, '{}'::jsonb, 0, 0)
                """,
                matchId,
                "it-" + matchId);
        jdbc.update(
                """
                INSERT INTO match_players (match_id, player_id, display_name, side)
                VALUES (?, ?, 'Home', 'HOME'), (?, ?, 'Away', 'AWAY')
                """,
                matchId,
                home,
                matchId,
                away);
        PointMatchSnapshot snapshot = store.loadSnapshot(matchId);
        return new SeededMatch(matchId, home, away, snapshot);
    }

    private record SeededMatch(UUID matchId, UUID home, UUID away, PointMatchSnapshot snapshot) {}
}
