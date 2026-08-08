package dev.sahilbasumatary.analyticsservice.index;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class PlayerMatchDocument {

    @Id
    private String id;
    private UUID playerId;
    private UUID matchId;
    private UUID opponentId;
    private String opponentName;
    private String side;
    private Boolean won;
    private String surface;
    private String tournamentKey;
    private String tournamentName;
    private Integer season;
    private String status;
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant endedAt;
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant scheduledAt;
    private TapeSideMetrics metrics;
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant indexedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public UUID getOpponentId() {
        return opponentId;
    }

    public void setOpponentId(UUID opponentId) {
        this.opponentId = opponentId;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Boolean getWon() {
        return won;
    }

    public void setWon(Boolean won) {
        this.won = won;
    }

    public String getSurface() {
        return surface;
    }

    public void setSurface(String surface) {
        this.surface = surface;
    }

    public String getTournamentKey() {
        return tournamentKey;
    }

    public void setTournamentKey(String tournamentKey) {
        this.tournamentKey = tournamentKey;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public TapeSideMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(TapeSideMetrics metrics) {
        this.metrics = metrics;
    }

    public Instant getIndexedAt() {
        return indexedAt;
    }

    public void setIndexedAt(Instant indexedAt) {
        this.indexedAt = indexedAt;
    }
}
