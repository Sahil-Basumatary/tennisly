package dev.sahilbasumatary.matchservice.service;

import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.exception.InvalidMatchStateException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MatchStateMachine {

    private static final Map<MatchStatus, Set<MatchStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(MatchStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(
                MatchStatus.SCHEDULED,
                EnumSet.of(MatchStatus.IN_PROGRESS, MatchStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(
                MatchStatus.IN_PROGRESS,
                EnumSet.of(MatchStatus.SUSPENDED, MatchStatus.COMPLETED, MatchStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(
                MatchStatus.SUSPENDED,
                EnumSet.of(MatchStatus.IN_PROGRESS, MatchStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(MatchStatus.COMPLETED, EnumSet.noneOf(MatchStatus.class));
        ALLOWED_TRANSITIONS.put(MatchStatus.CANCELLED, EnumSet.noneOf(MatchStatus.class));
    }

    public void assertCanTransition(MatchStatus currentStatus, MatchStatus nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus)) {
            throw new InvalidMatchStateException(
                    "Cannot transition match from " + currentStatus + " to " + nextStatus);
        }
    }

    public void assertCanRecordPoint(MatchStatus status) {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new InvalidMatchStateException(
                    "Points can only be recorded while a match is in progress");
        }
    }
}
