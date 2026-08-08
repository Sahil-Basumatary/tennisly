package dev.sahilbasumatary.analyticsservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchPlayerSummary(UUID playerId, String displayName, String side) {}
