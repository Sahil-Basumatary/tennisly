package dev.sahilbasumatary.matchservice.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.matchservice.dto.request.RecordPointRequest;
import dev.sahilbasumatary.matchservice.dto.response.MatchPlayerResponse;
import dev.sahilbasumatary.matchservice.entity.MatchEventType;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.PlayerSide;
import dev.sahilbasumatary.matchservice.entity.Surface;
import dev.sahilbasumatary.matchservice.exception.ResourceNotFoundException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MatchPointCommitStore {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private static final String LOAD_MATCH_SQL =
            """
            SELECT id, external_id, tournament_id, surface, status, best_of_sets,
                   scheduled_at, started_at, ended_at, metadata, current_score,
                   point_count, live_sequence, created_at, updated_at
            FROM matches
            WHERE id = ?
            """;

    private static final String LOAD_PLAYERS_SQL =
            """
            SELECT id, player_id, display_name, side, seed_number
            FROM match_players
            WHERE match_id = ?
            ORDER BY side ASC
            """;

    private static final String UPDATE_MATCH_SQL =
            """
            UPDATE matches
            SET point_count = point_count + 1,
                live_sequence = live_sequence + 1,
                current_score = ?::jsonb,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            RETURNING point_count, live_sequence, updated_at
            """;

    private static final String INSERT_POINT_SQL =
            """
            INSERT INTO match_points (
                id, match_id, sequence_number, server_id, winner_id, outcome,
                rally_length, score_snapshot, shot_summary, recorded_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, CURRENT_TIMESTAMP)
            RETURNING recorded_at
            """;

    private static final String INSERT_EVENT_SQL =
            """
            INSERT INTO match_event_logs (
                id, match_id, event_type, sequence_number, payload, created_at)
            VALUES (?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)
            """;

    private static final String INSERT_OUTBOX_SQL =
            """
            INSERT INTO match_outbox (
                id, event_json, status, attempts, available_at, created_at)
            VALUES (?, ?::jsonb, 'PENDING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MatchPointCommitStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PointMatchSnapshot loadSnapshot(UUID matchId) {
        List<PointMatchSnapshot> matches =
                jdbcTemplate.query(LOAD_MATCH_SQL, (rs, rowNum) -> mapMatch(rs), matchId);
        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("Match", matchId);
        }
        List<MatchPlayerResponse> players =
                jdbcTemplate.query(LOAD_PLAYERS_SQL, (rs, rowNum) -> mapPlayer(rs), matchId);
        PointMatchSnapshot match = matches.get(0);
        return new PointMatchSnapshot(
                match.id(),
                match.externalId(),
                match.tournamentId(),
                match.surface(),
                match.status(),
                match.bestOfSets(),
                match.scheduledAt(),
                match.startedAt(),
                match.endedAt(),
                match.metadata(),
                match.currentScore(),
                match.pointCount(),
                match.liveSequence(),
                match.createdAt(),
                match.updatedAt(),
                List.copyOf(players));
    }

    @Transactional
    public CommittedPoint commit(
            PointMatchSnapshot snapshot, RecordPointRequest request, MatchEvent event) {
        UUID pointId = UUID.randomUUID();
        UUID eventLogId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();
        String scoreJson = json(request.scoreSnapshot());
        String shotJson = json(request.shotSummary() == null ? Map.of() : request.shotSummary());
        return jdbcTemplate.execute(
                (ConnectionCallback<CommittedPoint>)
                        connection -> {
                            int sequence;
                            long liveSequence;
                            Instant updatedAt;
                            try (PreparedStatement update =
                                    connection.prepareStatement(UPDATE_MATCH_SQL)) {
                                update.setString(1, scoreJson);
                                update.setObject(2, snapshot.id());
                                try (ResultSet rs = update.executeQuery()) {
                                    if (!rs.next()) {
                                        throw new ResourceNotFoundException("Match", snapshot.id());
                                    }
                                    sequence = rs.getInt("point_count");
                                    liveSequence = rs.getLong("live_sequence");
                                    updatedAt = rs.getTimestamp("updated_at").toInstant();
                                }
                            }
                            Instant recordedAt;
                            try (PreparedStatement insertPoint =
                                    connection.prepareStatement(INSERT_POINT_SQL)) {
                                insertPoint.setObject(1, pointId);
                                insertPoint.setObject(2, snapshot.id());
                                insertPoint.setInt(3, sequence);
                                insertPoint.setObject(4, request.serverId());
                                insertPoint.setObject(5, request.winnerId());
                                insertPoint.setString(6, request.outcome().name());
                                if (request.rallyLength() == null) {
                                    insertPoint.setObject(7, null);
                                } else {
                                    insertPoint.setInt(7, request.rallyLength());
                                }
                                insertPoint.setString(8, scoreJson);
                                insertPoint.setString(9, shotJson);
                                try (ResultSet rs = insertPoint.executeQuery()) {
                                    if (!rs.next()) {
                                        throw new IllegalStateException(
                                                "point insert did not return recorded_at");
                                    }
                                    recordedAt = rs.getTimestamp("recorded_at").toInstant();
                                }
                            }
                            event.setSequence(liveSequence);
                            event.setPointSequence(sequence);
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("sequenceNumber", sequence);
                            payload.put("serverId", request.serverId().toString());
                            payload.put("winnerId", request.winnerId().toString());
                            payload.put("outcome", request.outcome().name());
                            payload.put("rallyLength", request.rallyLength());
                            payload.put("scoreSnapshot", request.scoreSnapshot());
                            try (PreparedStatement insertEvent =
                                    connection.prepareStatement(INSERT_EVENT_SQL)) {
                                insertEvent.setObject(1, eventLogId);
                                insertEvent.setObject(2, snapshot.id());
                                insertEvent.setString(3, MatchEventType.POINT_RECORDED.name());
                                insertEvent.setLong(4, liveSequence);
                                insertEvent.setString(5, json(payload));
                                insertEvent.executeUpdate();
                            }
                            try (PreparedStatement insertOutbox =
                                    connection.prepareStatement(INSERT_OUTBOX_SQL)) {
                                insertOutbox.setObject(1, outboxId);
                                insertOutbox.setString(2, json(event));
                                insertOutbox.executeUpdate();
                            }
                            return new CommittedPoint(
                                    pointId,
                                    sequence,
                                    liveSequence,
                                    recordedAt,
                                    updatedAt,
                                    request.scoreSnapshot());
                        });
    }

    private PointMatchSnapshot mapMatch(ResultSet rs) throws SQLException {
        return new PointMatchSnapshot(
                rs.getObject("id", UUID.class),
                rs.getString("external_id"),
                rs.getObject("tournament_id", UUID.class),
                Surface.valueOf(rs.getString("surface")),
                MatchStatus.valueOf(rs.getString("status")),
                rs.getInt("best_of_sets"),
                instant(rs.getTimestamp("scheduled_at")),
                instant(rs.getTimestamp("started_at")),
                instant(rs.getTimestamp("ended_at")),
                jsonMap(rs.getString("metadata")),
                jsonMap(rs.getString("current_score")),
                rs.getInt("point_count"),
                rs.getLong("live_sequence"),
                instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("updated_at")),
                List.of());
    }

    private MatchPlayerResponse mapPlayer(ResultSet rs) throws SQLException {
        return new MatchPlayerResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("player_id", UUID.class),
                rs.getString("display_name"),
                PlayerSide.valueOf(rs.getString("side")),
                (Integer) rs.getObject("seed_number"));
    }

    private Map<String, Object> jsonMap(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("unable to parse match json", ex);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("unable to serialise match json", ex);
        }
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
