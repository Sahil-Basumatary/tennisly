package dev.sahilbasumatary.matchservice.dto.response;

import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import java.util.UUID;

public record MatchCursorResponse(
        UUID id, MatchStatus status, long liveSequence, int pointsPlayed) {

    public static MatchCursorResponse from(MatchResponse match) {
        return new MatchCursorResponse(
                match.id(), match.status(), match.liveSequence(), match.pointsPlayed());
    }
}
