package dev.sahilbasumatary.replayservice.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.replayservice.dto.response.ReplayArtifactResponse;
import dev.sahilbasumatary.replayservice.service.ReplayArtifactService;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MatchCompletedReplayListenerTest {

    private static final UUID MATCH_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void materializesWhenStatusChangesToCompleted() {
        AtomicReference<UUID> seen = new AtomicReference<>();
        MatchCompletedReplayListener listener =
                new MatchCompletedReplayListener(recordingService(seen));
        listener.onMatchEvent(MatchEvent.statusChanged(MATCH_ID, "COMPLETED"));
        assertEquals(MATCH_ID, seen.get());
    }

    @Test
    void ignoresNonCompletedStatusChanges() {
        AtomicReference<UUID> seen = new AtomicReference<>();
        MatchCompletedReplayListener listener =
                new MatchCompletedReplayListener(recordingService(seen));
        listener.onMatchEvent(MatchEvent.statusChanged(MATCH_ID, "IN_PROGRESS"));
        assertNull(seen.get());
    }

    @Test
    void ignoresPointRecordedEvents() {
        AtomicReference<UUID> seen = new AtomicReference<>();
        MatchCompletedReplayListener listener =
                new MatchCompletedReplayListener(recordingService(seen));
        listener.onMatchEvent(
                MatchEvent.pointRecorded(MATCH_ID, "IN_PROGRESS", 1, MATCH_ID, "WINNER"));
        assertNull(seen.get());
    }

    private static ReplayArtifactService recordingService(AtomicReference<UUID> seen) {
        return new ReplayArtifactService(null, null, null, null, null) {
            @Override
            public ReplayArtifactResponse materialize(UUID matchId) {
                seen.set(matchId);
                return null;
            }
        };
    }
}
