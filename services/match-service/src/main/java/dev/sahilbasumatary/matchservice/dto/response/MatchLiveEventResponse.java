package dev.sahilbasumatary.matchservice.dto.response;

import dev.sahilbasumatary.common.event.MatchEvent;
import java.time.Instant;
import java.util.UUID;

public record MatchLiveEventResponse(
        String eventId,
        String eventType,
        UUID matchId,
        long sequence,
        Instant occurredAt,
        Instant commitObservedAt,
        MatchResponse snapshot) {

    public static MatchLiveEventResponse from(
            MatchEvent event, MatchResponse snapshot, Instant commitObservedAt) {
        return new MatchLiveEventResponse(
                event.getEventId(),
                event.getEventType(),
                event.getMatchId(),
                event.getSequence(),
                event.getTimestamp(),
                commitObservedAt,
                snapshot);
    }
}
