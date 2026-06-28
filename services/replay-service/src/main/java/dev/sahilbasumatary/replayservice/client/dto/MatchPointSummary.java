package dev.sahilbasumatary.replayservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchPointSummary(
        UUID id,
        int sequenceNumber,
        UUID serverId,
        UUID winnerId,
        String outcome,
        int rallyLength,
        Map<String, Object> scoreSnapshot,
        Map<String, Object> shotSummary) {}
