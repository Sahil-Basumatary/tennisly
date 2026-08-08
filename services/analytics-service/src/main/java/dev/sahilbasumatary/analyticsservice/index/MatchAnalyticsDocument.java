package dev.sahilbasumatary.analyticsservice.index;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class MatchAnalyticsDocument {

    @Id
    private String id;
    private UUID matchId;
    private String externalId;
    private UUID tournamentId;
    private String tournamentKey;
    private String tournamentName;
    private Integer season;
    private String surface;
    private String status;
    private int bestOfSets;
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant scheduledAt;
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant startedAt;
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant endedAt;
    private UUID homePlayerId;
    private String homeDisplayName;
    private UUID awayPlayerId;
    private String awayDisplayName;
    private UUID winnerPlayerId;
    private TapeSideMetrics homeMetrics;
    private TapeSideMetrics awayMetrics;
    private int pointsPlayed;
    private Map<String, Object> scoreSnapshot;
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant indexedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public UUID getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(UUID tournamentId) {
        this.tournamentId = tournamentId;
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

    public String getSurface() {
        return surface;
    }

    public void setSurface(String surface) {
        this.surface = surface;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getBestOfSets() {
        return bestOfSets;
    }

    public void setBestOfSets(int bestOfSets) {
        this.bestOfSets = bestOfSets;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public UUID getHomePlayerId() {
        return homePlayerId;
    }

    public void setHomePlayerId(UUID homePlayerId) {
        this.homePlayerId = homePlayerId;
    }

    public String getHomeDisplayName() {
        return homeDisplayName;
    }

    public void setHomeDisplayName(String homeDisplayName) {
        this.homeDisplayName = homeDisplayName;
    }

    public UUID getAwayPlayerId() {
        return awayPlayerId;
    }

    public void setAwayPlayerId(UUID awayPlayerId) {
        this.awayPlayerId = awayPlayerId;
    }

    public String getAwayDisplayName() {
        return awayDisplayName;
    }

    public void setAwayDisplayName(String awayDisplayName) {
        this.awayDisplayName = awayDisplayName;
    }

    public UUID getWinnerPlayerId() {
        return winnerPlayerId;
    }

    public void setWinnerPlayerId(UUID winnerPlayerId) {
        this.winnerPlayerId = winnerPlayerId;
    }

    public TapeSideMetrics getHomeMetrics() {
        return homeMetrics;
    }

    public void setHomeMetrics(TapeSideMetrics homeMetrics) {
        this.homeMetrics = homeMetrics;
    }

    public TapeSideMetrics getAwayMetrics() {
        return awayMetrics;
    }

    public void setAwayMetrics(TapeSideMetrics awayMetrics) {
        this.awayMetrics = awayMetrics;
    }

    public int getPointsPlayed() {
        return pointsPlayed;
    }

    public void setPointsPlayed(int pointsPlayed) {
        this.pointsPlayed = pointsPlayed;
    }

    public Map<String, Object> getScoreSnapshot() {
        return scoreSnapshot;
    }

    public void setScoreSnapshot(Map<String, Object> scoreSnapshot) {
        this.scoreSnapshot = scoreSnapshot;
    }

    public Instant getIndexedAt() {
        return indexedAt;
    }

    public void setIndexedAt(Instant indexedAt) {
        this.indexedAt = indexedAt;
    }
}
