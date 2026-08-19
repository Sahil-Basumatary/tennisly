package dev.sahilbasumatary.analyticsservice.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.sahilbasumatary.common.event.MatchEvent;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchEventAnalyticsHandlerTest {

    @Mock private MatchAnalyticsIngestionService ingestionService;

    private MatchEventAnalyticsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MatchEventAnalyticsHandler(ingestionService);
    }

    @Test
    void completedMatchIsProcessed() {
        MatchEvent event =
                MatchEvent.statusChanged(
                        UUID.fromString("cccccccc-dddd-eeee-ffff-000000000000"), "COMPLETED");
        handler.handle(event);
        verify(ingestionService).processEvent(event);
    }

    @Test
    void liveStatusChangeIsIgnored() {
        MatchEvent event =
                MatchEvent.statusChanged(
                        UUID.fromString("cccccccc-dddd-eeee-ffff-000000000000"), "IN_PROGRESS");
        handler.handle(event);
        verify(ingestionService, never()).processEvent(event);
    }

    @Test
    void pointRecordedIsProcessed() {
        UUID matchId = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000000");
        UUID winnerId = UUID.fromString("dddddddd-eeee-ffff-0000-111111111111");
        MatchEvent event = MatchEvent.pointRecorded(matchId, "IN_PROGRESS", 1, winnerId, "15-0");
        handler.handle(event);
        verify(ingestionService).processEvent(event);
    }
}
