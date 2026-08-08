package dev.sahilbasumatary.common.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "eventType",
        visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserEvent.class, names = {
            "USER_CREATED", "USER_UPDATED", "USER_DELETED"
    }),
    @JsonSubTypes.Type(value = OrganizationEvent.class, names = {
            "ORGANIZATION_CREATED", "ORGANIZATION_UPDATED", "ORGANIZATION_DELETED"
    }),
    @JsonSubTypes.Type(value = TennisDataEvent.class, names = {
            "TENNIS_PLAYERS_SYNCED", "TENNIS_TOURNAMENTS_SYNCED", "TENNIS_RANKINGS_SYNCED"
    }),
    @JsonSubTypes.Type(value = MatchEvent.class, names = {
            "MATCH_CREATED", "MATCH_UPDATED", "MATCH_STATUS_CHANGED", "MATCH_POINT_RECORDED"
    }),
    @JsonSubTypes.Type(value = WebhookDomainEvent.class, names = {
            "WEBHOOK_DISPATCH"
    })
})
public abstract class BaseEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String source;
    private int version;

    protected BaseEvent() {}

    protected BaseEvent(String eventType, String source) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = Instant.now();
        this.source = source;
        this.version = 1;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
