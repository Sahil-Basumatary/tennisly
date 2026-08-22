package dev.sahilbasumatary.matchservice.dto.response;

import dev.sahilbasumatary.matchservice.entity.MatchEventLog;
import dev.sahilbasumatary.matchservice.entity.MatchEventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MatchEventLogResponse(
        UUID id,
        long sequence,
        MatchEventType eventType,
        Map<String, Object> payload,
        Instant createdAt) {

    public static MatchEventLogResponse from(MatchEventLog eventLog) {
        return new MatchEventLogResponse(
                eventLog.getId(),
                eventLog.getSequenceNumber(),
                eventLog.getEventType(),
                eventLog.getPayload(),
                eventLog.getCreatedAt());
    }
}
