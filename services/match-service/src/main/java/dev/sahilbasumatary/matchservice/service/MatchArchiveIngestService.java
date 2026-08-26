package dev.sahilbasumatary.matchservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.matchservice.dto.request.CreateArchiveJobRequest;
import dev.sahilbasumatary.matchservice.dto.request.RecordPointBatchRequest;
import dev.sahilbasumatary.matchservice.dto.request.RecordPointRequest;
import dev.sahilbasumatary.matchservice.dto.response.ArchiveBatchResponse;
import dev.sahilbasumatary.matchservice.dto.response.ArchiveJobResponse;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.exception.InvalidMatchStateException;
import dev.sahilbasumatary.matchservice.repository.MatchPointCommitStore;
import dev.sahilbasumatary.matchservice.repository.PointMatchSnapshot;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class MatchArchiveIngestService {

    private static final int MAX_BATCH_POINTS = 1_000;

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final MatchPointCommitStore pointCommitStore;
    private final MatchStateMachine stateMachine;
    private final MatchOutboxWriter outboxWriter;
    private final int maxRows;
    private final long maxBytes;

    public MatchArchiveIngestService(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource,
            ObjectMapper objectMapper,
            MatchPointCommitStore pointCommitStore,
            MatchStateMachine stateMachine,
            MatchOutboxWriter outboxWriter,
            @Value("${tennisly.archive.max-rows:1000000}") int maxRows,
            @Value("${tennisly.archive.max-bytes:268435456}") long maxBytes) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.pointCommitStore = pointCommitStore;
        this.stateMachine = stateMachine;
        this.outboxWriter = outboxWriter;
        this.maxRows = maxRows;
        this.maxBytes = maxBytes;
    }

    @Transactional
    public ArchiveBatchResponse ingestBatch(UUID matchId, RecordPointBatchRequest request) {
        List<RecordPointRequest> points = request.points();
        if (points == null || points.isEmpty() || points.size() > MAX_BATCH_POINTS) {
            throw new InvalidMatchStateException(
                    "archive batches must contain 1.." + MAX_BATCH_POINTS + " points");
        }
        String idempotencyKey = blankToNull(request.idempotencyKey());
        if (idempotencyKey != null) {
            ArchiveBatchResponse replayed = findCompletedBatch(matchId, idempotencyKey);
            if (replayed != null) {
                return replayed;
            }
        }
        PointMatchSnapshot snapshot = pointCommitStore.loadSnapshot(matchId);
        if (snapshot.status() != MatchStatus.IN_PROGRESS
                && snapshot.status() != MatchStatus.COMPLETED) {
            throw new InvalidMatchStateException(
                    "Archive batches require an in-progress or completed match");
        }
        if (snapshot.status() == MatchStatus.IN_PROGRESS) {
            stateMachine.assertCanRecordPoint(snapshot.status());
        }
        for (RecordPointRequest point : points) {
            if (!snapshot.hasPlayer(point.serverId()) || !snapshot.hasPlayer(point.winnerId())) {
                throw new InvalidMatchStateException(
                        "batch contains a player who is not in the match");
            }
        }
        BatchReservation reservation = reserveBatchJob(matchId, idempotencyKey, points.size());
        if (reservation.replay() != null) {
            return reservation.replay();
        }
        UUID jobId = reservation.jobId();
        int count = points.size();
        long[] counters =
                jdbcTemplate.query(
                        """
                        UPDATE matches
                        SET point_count = point_count + ?,
                            live_sequence = live_sequence + ?,
                            current_score = ?::jsonb,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        RETURNING point_count, live_sequence
                        """,
                        rs -> {
                            if (!rs.next()) {
                                return null;
                            }
                            return new long[] {rs.getLong(1), rs.getLong(2)};
                        },
                        count,
                        count,
                        json(points.get(count - 1).scoreSnapshot()),
                        matchId);
        if (counters == null) {
            throw new InvalidMatchStateException("match disappeared during archive batch");
        }
        int lastSequence = (int) counters[0];
        int firstSequence = lastSequence - count + 1;
        jdbcTemplate.execute(
                (ConnectionCallback<Void>)
                        connection -> {
                            try (PreparedStatement insert =
                                    connection.prepareStatement(
                                            """
                                            INSERT INTO match_points (
                                                id, match_id, sequence_number, server_id, winner_id,
                                                outcome, rally_length, score_snapshot, shot_summary, recorded_at)
                                            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, CURRENT_TIMESTAMP)
                                            """)) {
                                for (int index = 0; index < count; index++) {
                                    RecordPointRequest point = points.get(index);
                                    insert.setObject(1, UUID.randomUUID());
                                    insert.setObject(2, matchId);
                                    insert.setInt(3, firstSequence + index);
                                    insert.setObject(4, point.serverId());
                                    insert.setObject(5, point.winnerId());
                                    insert.setString(6, point.outcome().name());
                                    if (point.rallyLength() == null) {
                                        insert.setObject(7, null);
                                    } else {
                                        insert.setInt(7, point.rallyLength());
                                    }
                                    insert.setString(8, json(point.scoreSnapshot()));
                                    insert.setString(
                                            9,
                                            json(
                                                    point.shotSummary() == null
                                                            ? Map.of()
                                                            : point.shotSummary()));
                                    insert.addBatch();
                                }
                                insert.executeBatch();
                            }
                            return null;
                        });
        ArchiveBatchResponse response =
                new ArchiveBatchResponse(matchId, count, firstSequence, lastSequence, counters[1]);
        completeBatchJob(jobId, response);
        return response;
    }

    @Transactional
    public ArchiveJobResponse createJob(UUID matchId, CreateArchiveJobRequest request) {
        PointMatchSnapshot snapshot = pointCommitStore.loadSnapshot(matchId);
        if (snapshot.status() != MatchStatus.IN_PROGRESS
                && snapshot.status() != MatchStatus.COMPLETED) {
            throw new InvalidMatchStateException(
                    "Archive jobs require an in-progress or completed match");
        }
        CreateArchiveJobRequest body = request == null ? new CreateArchiveJobRequest(null, null, null, null) : request;
        long expectedRows = body.expectedRows() == null ? 0L : body.expectedRows();
        if (expectedRows < 0 || expectedRows > maxRows) {
            throw new InvalidMatchStateException("expectedRows must be 0.." + maxRows);
        }
        long expectedBytes = body.expectedBytes() == null ? 0L : body.expectedBytes();
        if (expectedBytes < 0 || expectedBytes > maxBytes) {
            throw new InvalidMatchStateException("expectedBytes must be 0.." + maxBytes);
        }
        String idempotencyKey = blankToNull(body.idempotencyKey());
        String expectedSha = blankToNull(body.expectedSha256());
        if (idempotencyKey != null) {
            ArchiveJobResponse existing = findJob(matchId, idempotencyKey);
            if (existing != null) {
                return existing;
            }
        }
        UUID jobId = UUID.randomUUID();
        UUID inserted =
                jdbcTemplate.query(
                        """
                        INSERT INTO match_archive_jobs (
                            id, status, match_id, idempotency_key, source_rows,
                            expected_bytes, expected_sha256)
                        VALUES (?, 'STAGING', ?, ?, ?, ?, ?)
                        ON CONFLICT (match_id, idempotency_key)
                            WHERE idempotency_key IS NOT NULL
                        DO NOTHING
                        RETURNING id
                        """,
                        rs -> rs.next() ? rs.getObject("id", UUID.class) : null,
                        jobId,
                        matchId,
                        idempotencyKey,
                        expectedRows,
                        expectedBytes == 0L ? null : expectedBytes,
                        expectedSha);
        if (inserted == null && idempotencyKey != null) {
            ArchiveJobResponse existing = findJob(matchId, idempotencyKey);
            if (existing != null) {
                return existing;
            }
            throw new InvalidMatchStateException("archive job already in progress for this key");
        }
        return getJob(inserted == null ? jobId : inserted);
    }

    @Transactional(readOnly = true)
    public ArchiveJobResponse getJob(UUID jobId) {
        List<ArchiveJobResponse> found =
                jdbcTemplate.query(
                        """
                        SELECT id, match_id, status, source_rows, accepted_rows, duplicate_rows,
                               bytes_received, checksum, content_sha256
                        FROM match_archive_jobs
                        WHERE id = ?
                        """,
                        (rs, rowNum) -> mapJob(rs),
                        jobId);
        if (found.isEmpty()) {
            throw new InvalidMatchStateException("archive job not found");
        }
        return found.get(0);
    }

    @Transactional
    public ArchiveJobResponse streamStaging(UUID jobId, InputStream body) {
        ArchiveJobResponse job = getJob(jobId);
        if ("COMPLETED".equals(job.status())) {
            return job;
        }
        if (!"STAGING".equals(job.status())
                && !"FAILED".equals(job.status())
                && !"STAGED".equals(job.status())) {
            throw new InvalidMatchStateException(
                    "archive stream requires STAGING, STAGED or FAILED, was " + job.status());
        }
        jdbcTemplate.update("DELETE FROM match_archive_staging WHERE job_id = ?", jobId);
        jdbcTemplate.update(
                """
                UPDATE match_archive_jobs
                SET status = 'STAGING',
                    bytes_received = 0,
                    content_sha256 = NULL,
                    last_error = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                jobId);
        DigestLimitingInputStream digesting = new DigestLimitingInputStream(body, maxBytes);
        try {
            long copied = copyStaging(digesting);
            if (copied > maxRows) {
                throw new InvalidMatchStateException("archive stream exceeded " + maxRows + " rows");
            }
            String sha = digesting.hex();
            JobLimits limits = loadLimits(jobId);
            if (limits == null) {
                throw new InvalidMatchStateException("archive job disappeared during stream");
            }
            if (limits.expectedBytes > 0 && limits.expectedBytes != digesting.count()) {
                throw new InvalidMatchStateException("archive byte count mismatch");
            }
            if (limits.expectedSha != null && !limits.expectedSha.equalsIgnoreCase(sha)) {
                throw new InvalidMatchStateException("archive content SHA-256 mismatch");
            }
            if (limits.expectedRows > 0 && limits.expectedRows != copied) {
                throw new InvalidMatchStateException("archive row count mismatch");
            }
            jdbcTemplate.update(
                    """
                    UPDATE match_archive_jobs
                    SET status = 'STAGED',
                        source_rows = ?,
                        bytes_received = ?,
                        content_sha256 = ?,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """,
                    copied,
                    digesting.count(),
                    sha,
                    jobId);
            return getJob(jobId);
        } catch (RuntimeException ex) {
            persistFailed(jobId, ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public ArchiveJobResponse promote(UUID jobId) {
        ArchiveJobResponse current = getJob(jobId);
        if ("COMPLETED".equals(current.status())) {
            return current;
        }
        if (!"STAGED".equals(current.status()) && !"PROMOTING".equals(current.status())) {
            throw new InvalidMatchStateException(
                    "promote requires STAGED or PROMOTING, was " + current.status());
        }
        int claimed =
                jdbcTemplate.update(
                        """
                        UPDATE match_archive_jobs
                        SET status = 'PROMOTING', updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND status IN ('STAGED', 'PROMOTING')
                        """,
                        jobId);
        if (claimed == 0) {
            return getJob(jobId);
        }
        int inserted =
                jdbcTemplate.update(
                        """
                        INSERT INTO match_points (
                            id, match_id, sequence_number, server_id, winner_id, outcome,
                            rally_length, score_snapshot, shot_summary, recorded_at)
                        SELECT gen_random_uuid(), match_id, sequence_number, server_id, winner_id, outcome,
                               rally_length, score_snapshot, shot_summary, CURRENT_TIMESTAMP
                        FROM match_archive_staging
                        WHERE job_id = ?
                        ON CONFLICT (match_id, sequence_number) DO NOTHING
                        """,
                        jobId);
        Number sourceNumber =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_archive_staging WHERE job_id = ?",
                        Number.class,
                        jobId);
        int source = sourceNumber == null ? 0 : sourceNumber.intValue();
        Number presentNumber =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM match_archive_staging AS staged
                        JOIN match_points AS points
                          ON points.match_id = staged.match_id
                         AND points.sequence_number = staged.sequence_number
                        WHERE staged.job_id = ?
                        """,
                        Number.class,
                        jobId);
        int present = presentNumber == null ? 0 : presentNumber.intValue();
        int duplicates = source - inserted;
        jdbcTemplate.update(
                """
                UPDATE matches AS match
                SET point_count = GREATEST(match.point_count, staged.max_seq),
                    live_sequence = GREATEST(match.live_sequence, staged.max_seq),
                    updated_at = CURRENT_TIMESTAMP
                FROM (
                    SELECT match_id, MAX(sequence_number) AS max_seq
                    FROM match_archive_staging
                    WHERE job_id = ?
                    GROUP BY match_id
                ) AS staged
                WHERE match.id = staged.match_id
                """,
                jobId);
        String checksum = sha256(jobId + ":" + source + ":" + present + ":" + current.contentSha256());
        jdbcTemplate.update(
                """
                UPDATE match_archive_jobs
                SET status = 'COMPLETED',
                    accepted_rows = ?,
                    duplicate_rows = ?,
                    checksum = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                present,
                duplicates,
                checksum,
                jobId);
        jdbcTemplate.update("DELETE FROM match_archive_staging WHERE job_id = ?", jobId);
        if (outboxWriter != null && current.matchId() != null) {
            outboxWriter.enqueue(
                    MatchEvent.archiveCompleted(current.matchId(), jobId, present, checksum));
        }
        return getJob(jobId);
    }

    private void persistFailed(UUID jobId, String message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            writeFailed(jobId, message);
                        }
                    });
            return;
        }
        writeFailed(jobId, message);
    }

    private void writeFailed(UUID jobId, String message) {
        String trimmed = message == null ? "archive stream failed" : message;
        if (trimmed.length() > 500) {
            trimmed = trimmed.substring(0, 500);
        }
        jdbcTemplate.update(
                """
                UPDATE match_archive_jobs
                SET status = 'FAILED',
                    last_error = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                trimmed,
                jobId);
    }

    private long copyStaging(InputStream tsv) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));
            return copyManager.copyIn(
                    """
                    COPY match_archive_staging (
                        job_id, match_id, sequence_number, server_id, winner_id,
                        outcome, rally_length, score_snapshot, shot_summary)
                    FROM STDIN WITH (FORMAT text)
                    """,
                    tsv);
        } catch (IOException | java.sql.SQLException ex) {
            throw new IllegalStateException("COPY into archive staging failed", ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private ArchiveJobResponse findJob(UUID matchId, String idempotencyKey) {
        List<ArchiveJobResponse> found =
                jdbcTemplate.query(
                        """
                        SELECT id, match_id, status, source_rows, accepted_rows, duplicate_rows,
                               bytes_received, checksum, content_sha256
                        FROM match_archive_jobs
                        WHERE match_id = ? AND idempotency_key = ?
                        """,
                        (rs, rowNum) -> mapJob(rs),
                        matchId,
                        idempotencyKey);
        return found.isEmpty() ? null : found.get(0);
    }

    private static ArchiveJobResponse mapJob(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ArchiveJobResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("match_id", UUID.class),
                rs.getString("status"),
                rs.getLong("source_rows"),
                rs.getLong("accepted_rows"),
                rs.getLong("duplicate_rows"),
                rs.getLong("bytes_received"),
                rs.getString("checksum"),
                rs.getString("content_sha256"));
    }

    private BatchReservation reserveBatchJob(UUID matchId, String idempotencyKey, int sourceRows) {
        if (idempotencyKey == null) {
            return new BatchReservation(null, null);
        }
        UUID jobId = UUID.randomUUID();
        UUID inserted =
                jdbcTemplate.query(
                        """
                        INSERT INTO match_archive_jobs (
                            id, status, match_id, idempotency_key, source_rows)
                        VALUES (?, 'STAGING', ?, ?, ?)
                        ON CONFLICT (match_id, idempotency_key)
                            WHERE idempotency_key IS NOT NULL
                        DO NOTHING
                        RETURNING id
                        """,
                        rs -> rs.next() ? rs.getObject("id", UUID.class) : null,
                        jobId,
                        matchId,
                        idempotencyKey,
                        sourceRows);
        if (inserted != null) {
            return new BatchReservation(inserted, null);
        }
        ArchiveBatchResponse existing = findCompletedBatch(matchId, idempotencyKey);
        if (existing != null) {
            return new BatchReservation(null, existing);
        }
        throw new InvalidMatchStateException(
                "archive batch already in progress for this idempotency key");
    }

    private void completeBatchJob(UUID jobId, ArchiveBatchResponse response) {
        if (jobId == null) {
            return;
        }
        jdbcTemplate.update(
                """
                UPDATE match_archive_jobs
                SET status = 'COMPLETED',
                    accepted_rows = ?,
                    first_sequence = ?,
                    last_sequence = ?,
                    live_sequence = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                response.accepted(),
                response.firstSequence(),
                response.lastSequence(),
                response.liveSequence(),
                jobId);
    }

    private ArchiveBatchResponse findCompletedBatch(UUID matchId, String idempotencyKey) {
        List<ArchiveBatchResponse> found =
                jdbcTemplate.query(
                        """
                        SELECT accepted_rows, first_sequence, last_sequence, live_sequence
                        FROM match_archive_jobs
                        WHERE match_id = ?
                          AND idempotency_key = ?
                          AND status = 'COMPLETED'
                        """,
                        (rs, rowNum) ->
                                new ArchiveBatchResponse(
                                        matchId,
                                        rs.getInt("accepted_rows"),
                                        rs.getInt("first_sequence"),
                                        rs.getInt("last_sequence"),
                                        rs.getLong("live_sequence")),
                        matchId,
                        idempotencyKey);
        return found.isEmpty() ? null : found.get(0);
    }

    private JobLimits loadLimits(UUID jobId) {
        return jdbcTemplate.query(
                "SELECT expected_bytes, expected_sha256, source_rows FROM match_archive_jobs WHERE id = ?",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Long bytes = rs.getObject("expected_bytes", Long.class);
                    return new JobLimits(
                            bytes == null ? 0L : bytes,
                            rs.getString("expected_sha256"),
                            rs.getLong("source_rows"));
                },
                jobId);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("unable to serialise archive json", ex);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 required for archive jobs", ex);
        }
    }

    private record BatchReservation(UUID jobId, ArchiveBatchResponse replay) {}

    private record JobLimits(long expectedBytes, String expectedSha, long expectedRows) {}

    static final class DigestLimitingInputStream extends FilterInputStream {
        private final MessageDigest digest;
        private final long maxBytes;
        private long count;

        DigestLimitingInputStream(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = maxBytes;
            try {
                this.digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException("SHA-256 required for archive streams", ex);
            }
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                accept(new byte[] {(byte) value}, 0, 1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            int n = super.read(buffer, off, len);
            if (n > 0) {
                accept(buffer, off, n);
            }
            return n;
        }

        private void accept(byte[] buffer, int off, int n) {
            count += n;
            if (count > maxBytes) {
                throw new InvalidMatchStateException("archive stream exceeded " + maxBytes + " bytes");
            }
            digest.update(buffer, off, n);
        }

        long count() {
            return count;
        }

        String hex() {
            return HexFormat.of().formatHex(digest.digest());
        }
    }
}
