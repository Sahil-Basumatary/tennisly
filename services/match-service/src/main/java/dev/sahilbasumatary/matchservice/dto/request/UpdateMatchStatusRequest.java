package dev.sahilbasumatary.matchservice.dto.request;

import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateMatchStatusRequest(
        @NotNull MatchStatus status, Map<String, Object> metadata) {}
