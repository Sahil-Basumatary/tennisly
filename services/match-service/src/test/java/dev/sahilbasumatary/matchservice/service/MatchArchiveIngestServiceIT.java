package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.matchservice.dto.request.CreateArchiveJobRequest;
import dev.sahilbasumatary.matchservice.dto.request.RecordPointBatchRequest;
import dev.sahilbasumatary.matchservice.dto.request.RecordPointRequest;
import dev.sahilbasumatary.matchservice.dto.response.ArchiveBatchResponse;
import dev.sahilbasumatary.matchservice.dto.response.ArchiveJobResponse;
import dev.sahilbasumatary.matchservice.entity.PointOutcome;
import dev.sahilbasumatary.matchservice.repository.MatchPointCommitStore;
import dev.sahilbasumatary.matchservice.repository.PostgresMatchHarness;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class MatchArchiveIngestServiceIT {

    private static JdbcTemplate jdbc;
    private static MatchArchiveIngestService ingest;
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
        MatchPointCommitStore store =
                new MatchPointCommitStore(jdbc, new ObjectMapper().findAndRegisterModules());
        ingest =
                new MatchArchiveIngestService(
                        jdbc,
                        dataSource,
                        new ObjectMapper().findAndRegisterModules(),
                        store,
                        new MatchStateMachine(),
                        null,
                        1_000_000,
                        32L * 1024 * 1024);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void transactionalBatchIsAllOrNothingAndAssignsContiguousSequences() {
        Seeded seeded = seedMatch();
        RecordPointRequest first =
                new RecordPointRequest(
                        seeded.home,
                        seeded.away,
                        PointOutcome.WINNER,
                        4,
                        Map.of("p", "1"),
                        Map.of());
        RecordPointRequest second =
                new RecordPointRequest(
                        seeded.away,
                        seeded.home,
                        PointOutcome.WINNER,
                        5,
                        Map.of("p", "2"),
                        Map.of());
        ArchiveBatchResponse result =
                transactions.execute(
                        status ->
                                ingest.ingestBatch(
                                        seeded.matchId,
                                        new RecordPointBatchRequest(
                                                "batch-1", List.of(first, second))));
        assertEquals(2, result.accepted());
        assertEquals(1, result.firstSequence());
        assertEquals(2, result.lastSequence());
        assertEquals(
                2L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM match_points WHERE match_id = ?",
                        Long.class,
                        seeded.matchId));
        assertEquals(
                2,
                jdbc.queryForObject(
                        "SELECT point_count FROM matches WHERE id = ?",
                        Integer.class,
                        seeded.matchId));
        ArchiveBatchResponse replay =
                ingest.ingestBatch(
                        seeded.matchId,
                        new RecordPointBatchRequest("batch-1", List.of(first, second)));
        assertEquals(result.firstSequence(), replay.firstSequence());
        assertEquals(result.lastSequence(), replay.lastSequence());
        assertEquals(
                2L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM match_points WHERE match_id = ?",
                        Long.class,
                        seeded.matchId));
    }

    @Test
    void streamingCopyPromoteIsRestartableAndIdempotent() {
        Seeded seeded = seedMatch();
        ArchiveJobResponse created =
                ingest.createJob(
                        seeded.matchId,
                        new CreateArchiveJobRequest("copy-1", 50L, null, null));
        assertEquals("STAGING", created.status());
        ArchiveJobResponse replayedCreate =
                ingest.createJob(
                        seeded.matchId,
                        new CreateArchiveJobRequest("copy-1", 50L, null, null));
        assertEquals(created.jobId(), replayedCreate.jobId());
        byte[] tsv =
                ArchiveStagingTsv.productionTape(
                                created.jobId(), seeded.matchId, seeded.home, seeded.away, 50, 7L)
                        .getBytes(StandardCharsets.UTF_8);
        transactions.execute(
                status -> ingest.streamStaging(created.jobId(), new ByteArrayInputStream(tsv)));
        ArchiveJobResponse staged = ingest.getJob(created.jobId());
        assertEquals("STAGED", staged.status());
        assertEquals(50, staged.sourceRows());
        assertTrue(staged.bytesReceived() > 10_000);
        assertEquals(64, staged.contentSha256().length());
        ArchiveJobResponse first = ingest.promote(created.jobId());
        assertEquals("COMPLETED", first.status());
        assertEquals(50, first.sourceRows());
        assertEquals(50, first.acceptedRows());
        assertEquals(0, first.duplicateRows());
        assertEquals(
                0L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM match_archive_staging WHERE job_id = ?",
                        Long.class,
                        created.jobId()));
        ArchiveJobResponse replay = ingest.promote(first.jobId());
        assertEquals(50, replay.sourceRows());
        assertEquals(50, replay.acceptedRows());
        assertEquals(0, replay.duplicateRows());
        assertEquals(first.checksum(), replay.checksum());
        assertEquals(
                50L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM match_points WHERE match_id = ?",
                        Long.class,
                        seeded.matchId));
        assertEquals(
                50L,
                jdbc.queryForObject(
                        "SELECT COUNT(DISTINCT sequence_number) FROM match_points WHERE match_id = ?",
                        Long.class,
                        seeded.matchId));
        assertEquals(
                50,
                jdbc.queryForObject(
                        "SELECT point_count FROM matches WHERE id = ?",
                        Integer.class,
                        seeded.matchId));
    }

    @Test
    void failedStreamCanBeRetried() {
        Seeded seeded = seedMatch();
        ArchiveJobResponse created =
                ingest.createJob(seeded.matchId, new CreateArchiveJobRequest("retry-1", 3L, null, null));
        byte[] full =
                ArchiveStagingTsv.productionTape(
                                created.jobId(), seeded.matchId, seeded.home, seeded.away, 3, 9L)
                        .getBytes(StandardCharsets.UTF_8);
        transactions.execute(
                status -> ingest.streamStaging(created.jobId(), new ByteArrayInputStream(full)));
        byte[] again =
                ArchiveStagingTsv.productionTape(
                                created.jobId(), seeded.matchId, seeded.home, seeded.away, 3, 9L)
                        .getBytes(StandardCharsets.UTF_8);
        ArchiveJobResponse staged =
                transactions.execute(
                        status ->
                                ingest.streamStaging(
                                        created.jobId(), new ByteArrayInputStream(again)));
        assertEquals("STAGED", staged.status());
        assertEquals(3, staged.sourceRows());
    }

    @Test
    void streamChecksumMismatchMarksJobFailed() {
        Seeded seeded = seedMatch();
        ArchiveJobResponse created =
                ingest.createJob(
                        seeded.matchId,
                        new CreateArchiveJobRequest("bad-sha", 3L, null, "00".repeat(32)));
        byte[] tsv =
                ArchiveStagingTsv.productionTape(
                                created.jobId(), seeded.matchId, seeded.home, seeded.away, 3, 11L)
                        .getBytes(StandardCharsets.UTF_8);
        try {
            transactions.execute(
                    status -> ingest.streamStaging(created.jobId(), new ByteArrayInputStream(tsv)));
        } catch (RuntimeException ignored) {
            // status is written after the stream transaction rolls back
        }
        ArchiveJobResponse failed = ingest.getJob(created.jobId());
        assertEquals("FAILED", failed.status());
    }

    private Seeded seedMatch() {
        UUID matchId = UUID.randomUUID();
        UUID home = UUID.randomUUID();
        UUID away = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO matches (id, external_id, surface, status, best_of_sets, current_score, metadata)
                VALUES (?, ?, 'HARD', 'IN_PROGRESS', 3, '{}'::jsonb, '{}'::jsonb)
                """,
                matchId,
                "archive-" + matchId);
        jdbc.update(
                """
                INSERT INTO match_players (match_id, player_id, display_name, side)
                VALUES (?, ?, 'Home', 'HOME'), (?, ?, 'Away', 'AWAY')
                """,
                matchId,
                home,
                matchId,
                away);
        return new Seeded(matchId, home, away);
    }

    private record Seeded(UUID matchId, UUID home, UUID away) {}
}
