package dev.sahilbasumatary.replayservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.replayservice.dto.response.ReplayArtifactResponse;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MatchCompletedReplayHandlerTest {

    private static final UUID MATCH_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void materializesWhenStatusChangesToCompleted() {
        AtomicReference<UUID> seen = new AtomicReference<>();
        MatchCompletedReplayHandler handler = new MatchCompletedReplayHandler(recordingService(seen));
        handler.handle(MatchEvent.statusChanged(MATCH_ID, "COMPLETED"));
        assertEquals(MATCH_ID, seen.get());
    }

    @Test
    void ignoresNonCompletedStatusChanges() {
        AtomicReference<UUID> seen = new AtomicReference<>();
        MatchCompletedReplayHandler handler = new MatchCompletedReplayHandler(recordingService(seen));
        handler.handle(MatchEvent.statusChanged(MATCH_ID, "IN_PROGRESS"));
        assertNull(seen.get());
    }

    @Test
    void ignoresPointRecordedEvents() {
        AtomicReference<UUID> seen = new AtomicReference<>();
        MatchCompletedReplayHandler handler = new MatchCompletedReplayHandler(recordingService(seen));
        handler.handle(MatchEvent.pointRecorded(MATCH_ID, "IN_PROGRESS", 1, MATCH_ID, "WINNER"));
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
