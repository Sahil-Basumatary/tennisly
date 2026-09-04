package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.matchservice.dto.response.MatchEventLogResponse;
import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchEventLog;
import dev.sahilbasumatary.matchservice.entity.MatchEventType;
import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import dev.sahilbasumatary.matchservice.repository.MatchEventLogRepository;
import dev.sahilbasumatary.matchservice.repository.MatchPointCommitStore;
import dev.sahilbasumatary.matchservice.repository.MatchPointRepository;
import dev.sahilbasumatary.matchservice.repository.MatchRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class MatchEventReplayTest {

    @Test
    void replaysOnlyEventsAfterTheClientCursorInSequenceOrder() {
        MatchRepository matchRepository = mock(MatchRepository.class);
        final MatchPointRepository pointRepository = mock(MatchPointRepository.class);
        final MatchEventLogRepository eventLogRepository = mock(MatchEventLogRepository.class);
        MatchEventReplayCache eventReplayCache = mock(MatchEventReplayCache.class);
        UUID matchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Match match = new Match();
        match.setId(matchId);
        MatchEventLog sixth = event(match, 6);
        MatchEventLog seventh = event(match, 7);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(eventReplayCache.read(matchId, 5, 2)).thenReturn(Optional.empty());
        when(eventLogRepository
                        .findByMatchIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                                matchId, 5, PageRequest.of(0, 2)))
                .thenReturn(List.of(sixth, seventh));
        MatchService service = service(matchRepository, pointRepository, eventLogRepository, eventReplayCache);

        List<MatchEventLogResponse> replay = service.listEvents(matchId, 5, 2);

        assertEquals(List.of(6L, 7L), replay.stream().map(MatchEventLogResponse::sequence).toList());
        verify(eventLogRepository)
                .findByMatchIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                        matchId, 5, PageRequest.of(0, 2));
        verify(eventReplayCache).write(eq(matchId), eq(5L), eq(2), anyList());
    }

    @Test
    void servesACoalescedRecoveryPageFromTheShortTtlCache() {
        MatchRepository matchRepository = mock(MatchRepository.class);
        final MatchEventLogRepository eventLogRepository = mock(MatchEventLogRepository.class);
        MatchEventReplayCache eventReplayCache = mock(MatchEventReplayCache.class);
        UUID matchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Match match = new Match();
        match.setId(matchId);
        MatchEventLogResponse cached =
                new MatchEventLogResponse(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        8L,
                        MatchEventType.POINT_RECORDED,
                        Map.of(),
                        Instant.parse("2026-08-26T12:00:00Z"));
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(eventReplayCache.read(matchId, 7, 2)).thenReturn(Optional.of(List.of(cached)));
        MatchService service =
                service(matchRepository, mock(MatchPointRepository.class), eventLogRepository, eventReplayCache);

        List<MatchEventLogResponse> replay = service.listEvents(matchId, 7, 2);

        assertEquals(List.of(8L), replay.stream().map(MatchEventLogResponse::sequence).toList());
        verify(eventLogRepository, never())
                .findByMatchIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                        matchId, 7, PageRequest.of(0, 2));
    }

    private MatchService service(
            MatchRepository matchRepository,
            MatchPointRepository pointRepository,
            MatchEventLogRepository eventLogRepository,
            MatchEventReplayCache eventReplayCache) {
        return new MatchService(
                matchRepository,
                pointRepository,
                eventLogRepository,
                mock(MatchStateMachine.class),
                mock(MatchEventLogService.class),
                mock(MatchRealtimeNotifier.class),
                mock(MatchEventDispatch.class),
                mock(MatchTimers.class),
                mock(MatchPointCommitStore.class),
                mock(MatchTickerCache.class),
                eventReplayCache);
    }

    private MatchEventLog event(Match match, long sequence) {
        MatchEventLog event = new MatchEventLog();
        event.setMatch(match);
        event.setSequenceNumber(sequence);
        event.setEventType(MatchEventType.POINT_RECORDED);
        return event;
    }
}
