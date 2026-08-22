package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.matchservice.dto.response.MatchEventLogResponse;
import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchEventLog;
import dev.sahilbasumatary.matchservice.entity.MatchEventType;
import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import dev.sahilbasumatary.matchservice.repository.MatchEventLogRepository;
import dev.sahilbasumatary.matchservice.repository.MatchPointRepository;
import dev.sahilbasumatary.matchservice.repository.MatchRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class MatchEventReplayTest {

    @Test
    void replaysOnlyEventsAfterTheClientCursorInSequenceOrder() {
        MatchRepository matchRepository = mock(MatchRepository.class);
        MatchPointRepository pointRepository = mock(MatchPointRepository.class);
        MatchEventLogRepository eventLogRepository = mock(MatchEventLogRepository.class);
        UUID matchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Match match = new Match();
        match.setId(matchId);
        MatchEventLog sixth = event(match, 6);
        MatchEventLog seventh = event(match, 7);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(eventLogRepository
                        .findByMatchIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                                matchId, 5, PageRequest.of(0, 2)))
                .thenReturn(List.of(sixth, seventh));
        MatchService service =
                new MatchService(
                        matchRepository,
                        pointRepository,
                        eventLogRepository,
                        mock(MatchStateMachine.class),
                        mock(MatchEventLogService.class),
                        mock(MatchRealtimeNotifier.class),
                        mock(MatchEventDispatch.class),
                        mock(MatchTimers.class));

        List<MatchEventLogResponse> replay = service.listEvents(matchId, 5, 2);

        assertEquals(List.of(6L, 7L), replay.stream().map(MatchEventLogResponse::sequence).toList());
        verify(eventLogRepository)
                .findByMatchIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                        matchId, 5, PageRequest.of(0, 2));
    }

    private MatchEventLog event(Match match, long sequence) {
        MatchEventLog event = new MatchEventLog();
        event.setMatch(match);
        event.setSequenceNumber(sequence);
        event.setEventType(MatchEventType.POINT_RECORDED);
        return event;
    }
}
