package dev.sahilbasumatary.common.event;

import java.util.UUID;

public class MatchEvent extends BaseEvent {

    public static final String MATCH_CREATED = "MATCH_CREATED";
    public static final String MATCH_UPDATED = "MATCH_UPDATED";
    public static final String MATCH_STATUS_CHANGED = "MATCH_STATUS_CHANGED";
    public static final String MATCH_POINT_RECORDED = "MATCH_POINT_RECORDED";

    private UUID matchId;
    private String status;
    private long sequence;
    private Integer pointSequence;
    private UUID winnerId;
    private String summary;

    public MatchEvent() {}

    public MatchEvent(String eventType, UUID matchId) {
        super(eventType, "match-service");
        this.matchId = matchId;
    }

    public static MatchEvent created(UUID matchId, String status) {
        MatchEvent event = new MatchEvent(MATCH_CREATED, matchId);
        event.status = status;
        return event;
    }

    public static MatchEvent updated(UUID matchId, String status) {
        MatchEvent event = new MatchEvent(MATCH_UPDATED, matchId);
        event.status = status;
        return event;
    }

    public static MatchEvent statusChanged(UUID matchId, String status) {
        MatchEvent event = new MatchEvent(MATCH_STATUS_CHANGED, matchId);
        event.status = status;
        return event;
    }

    public static MatchEvent pointRecorded(
            UUID matchId, String status, Integer pointSequence, UUID winnerId, String summary) {
        MatchEvent event = new MatchEvent(MATCH_POINT_RECORDED, matchId);
        event.status = status;
        event.pointSequence = pointSequence;
        event.winnerId = winnerId;
        event.summary = summary;
        return event;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public Integer getPointSequence() {
        return pointSequence;
    }

    public void setPointSequence(Integer pointSequence) {
        this.pointSequence = pointSequence;
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(UUID winnerId) {
        this.winnerId = winnerId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
