package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.matchservice.dto.request.RecordPointRequest;
import dev.sahilbasumatary.matchservice.dto.response.MatchPlayerResponse;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.PlayerSide;
import dev.sahilbasumatary.matchservice.entity.PointOutcome;
import dev.sahilbasumatary.matchservice.entity.Surface;
import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import dev.sahilbasumatary.matchservice.repository.CommittedPoint;
import dev.sahilbasumatary.matchservice.repository.MatchEventLogRepository;
import dev.sahilbasumatary.matchservice.repository.MatchPointCommitStore;
import dev.sahilbasumatary.matchservice.repository.MatchPointRepository;
import dev.sahilbasumatary.matchservice.repository.MatchRepository;
import dev.sahilbasumatary.matchservice.repository.PointMatchSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchServiceRecordPointTest {

    @Mock private MatchRepository matchRepository;
    @Mock private MatchPointRepository pointRepository;
    @Mock private MatchEventLogRepository eventLogRepository;
    @Mock private MatchEventLogService eventLogService;
    @Mock private MatchRealtimeNotifier realtimeNotifier;
    @Mock private MatchEventDispatch eventDispatch;
    @Mock private MatchPointCommitStore pointCommitStore;

    private MatchService matchService;
    private final UUID matchId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private final UUID home = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID away = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        matchService =
                new MatchService(
                        matchRepository,
                        pointRepository,
                        eventLogRepository,
                        new MatchStateMachine(),
                        eventLogService,
                        realtimeNotifier,
                        eventDispatch,
                        new MatchTimers(new SimpleMeterRegistry()),
                        pointCommitStore,
                        mock(MatchTickerCache.class),
                        mock(MatchEventReplayCache.class));
    }

    @Test
    void recordsPointThroughAtomicCommitThenFansOutWithoutASecondOutboxWrite() {
        PointMatchSnapshot snapshot = snapshot(MatchStatus.IN_PROGRESS);
        when(pointCommitStore.loadSnapshot(matchId)).thenReturn(snapshot);
        when(pointCommitStore.commit(eq(snapshot), any(), any()))
                .thenReturn(
                        new CommittedPoint(
                                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                                1,
                                1L,
                                Instant.parse("2026-08-25T12:00:00Z"),
                                Instant.parse("2026-08-25T12:00:00Z"),
                                Map.of("game", "15-0")));

        var response = matchService.recordPoint(matchId, request());

        assertEquals(1, response.sequenceNumber());
        verify(eventDispatch).fanoutAfterCommit(eq(matchId), any(MatchEvent.class), any());
        verify(eventDispatch, never()).publish(any(), any(), any());
        verify(pointRepository, never()).save(any());
    }

    @Test
    void doesNotFanoutWhenAtomicCommitFails() {
        PointMatchSnapshot snapshot = snapshot(MatchStatus.IN_PROGRESS);
        when(pointCommitStore.loadSnapshot(matchId)).thenReturn(snapshot);
        when(pointCommitStore.commit(eq(snapshot), any(), any()))
                .thenThrow(new IllegalStateException("forced rollback"));

        assertThrows(
                IllegalStateException.class, () -> matchService.recordPoint(matchId, request()));
        verify(eventDispatch, never()).fanoutAfterCommit(any(), any(), any());
        verify(eventDispatch, never()).publish(any(), any(), any());
    }

    private RecordPointRequest request() {
        return new RecordPointRequest(
                home, away, PointOutcome.WINNER, 4, Map.of("game", "15-0"), Map.of());
    }

    private PointMatchSnapshot snapshot(MatchStatus status) {
        return new PointMatchSnapshot(
                matchId,
                "atomic-1",
                null,
                Surface.HARD,
                status,
                3,
                Instant.parse("2026-08-25T11:00:00Z"),
                Instant.parse("2026-08-25T11:05:00Z"),
                null,
                Map.of(),
                Map.of(),
                0,
                0L,
                Instant.parse("2026-08-25T11:00:00Z"),
                Instant.parse("2026-08-25T11:05:00Z"),
                List.of(
                        new MatchPlayerResponse(
                                UUID.randomUUID(), home, "Home", PlayerSide.HOME, 1),
                        new MatchPlayerResponse(
                                UUID.randomUUID(), away, "Away", PlayerSide.AWAY, 2)));
    }
}
