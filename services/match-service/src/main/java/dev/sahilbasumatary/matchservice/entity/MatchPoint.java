package dev.sahilbasumatary.matchservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "match_points")
@EntityListeners(AuditingEntityListener.class)
public class MatchPoint {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "server_id", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID serverId;

    @Column(name = "winner_id", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID winnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PointOutcome outcome;

    @Column(name = "rally_length")
    private Integer rallyLength;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> scoreSnapshot = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shot_summary", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> shotSummary = new HashMap<>();

    @CreatedDate
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public UUID getServerId() {
        return serverId;
    }

    public void setServerId(UUID serverId) {
        this.serverId = serverId;
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(UUID winnerId) {
        this.winnerId = winnerId;
    }

    public PointOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(PointOutcome outcome) {
        this.outcome = outcome;
    }

    public Integer getRallyLength() {
        return rallyLength;
    }

    public void setRallyLength(Integer rallyLength) {
        this.rallyLength = rallyLength;
    }

    public Map<String, Object> getScoreSnapshot() {
        return scoreSnapshot;
    }

    public void setScoreSnapshot(Map<String, Object> scoreSnapshot) {
        this.scoreSnapshot = scoreSnapshot == null ? new HashMap<>() : scoreSnapshot;
    }

    public Map<String, Object> getShotSummary() {
        return shotSummary;
    }

    public void setShotSummary(Map<String, Object> shotSummary) {
        this.shotSummary = shotSummary == null ? new HashMap<>() : shotSummary;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
