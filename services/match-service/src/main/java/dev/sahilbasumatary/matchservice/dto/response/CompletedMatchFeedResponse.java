package dev.sahilbasumatary.matchservice.dto.response;

import java.util.List;
import java.util.UUID;

public record CompletedMatchFeedResponse(List<UUID> matchIds, UUID nextCursor, boolean hasMore) {}
