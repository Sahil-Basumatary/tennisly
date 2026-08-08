package dev.sahilbasumatary.analyticsservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompletedMatchFeedResponse(List<UUID> matchIds, UUID nextCursor, boolean hasMore) {}
