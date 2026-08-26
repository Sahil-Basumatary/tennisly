package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.Surface;
import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import dev.sahilbasumatary.matchservice.repository.MatchEventLogRepository;
import dev.sahilbasumatary.matchservice.repository.MatchPointCommitStore;
import dev.sahilbasumatary.matchservice.repository.MatchPointRepository;
import dev.sahilbasumatary.matchservice.repository.MatchRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MatchListQueryTest {

    @Mock private MatchRepository matchRepository;
    @Mock private MatchPointRepository pointRepository;
    @Mock private MatchEventLogRepository eventLogRepository;
    @Mock private MatchStateMachine stateMachine;
    @Mock private MatchEventLogService eventLogService;
    @Mock private MatchRealtimeNotifier realtimeNotifier;
    @Mock private MatchEventDispatch eventDispatch;
    @Mock private MatchPointCommitStore pointCommitStore;
    @Mock private MatchTickerCache tickerCache;
    @Mock private MatchEventReplayCache eventReplayCache;

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService =
                new MatchService(
                        matchRepository,
                        pointRepository,
                        eventLogRepository,
                        stateMachine,
                        eventLogService,
                        realtimeNotifier,
                        eventDispatch,
                        new MatchTimers(new SimpleMeterRegistry()),
                        pointCommitStore,
                        tickerCache,
                        eventReplayCache);
    }

    @Test
    void listUsesEntityGraphPageAndSkipsPointCountQueries() {
        Match match = new Match();
        match.setId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
        match.setSurface(Surface.HARD);
        match.setStatus(MatchStatus.SCHEDULED);
        match.setPointCount(4);
        when(matchRepository.findAllByOrderByScheduledAtAsc(any(Pageable.class)))
                .thenReturn(List.of(match));

        var rows = matchService.listMatches(null, null, 0, 20);

        assertEquals(1, rows.size());
        assertEquals(4, rows.get(0).pointsPlayed());
        verify(matchRepository).findAllByOrderByScheduledAtAsc(any(Pageable.class));
        verify(pointRepository, never()).countByMatchId(any());
        verify(pointRepository, never()).countGroupedByMatchIds(any());
    }

    @Test
    void liveListStartsWithTheNewestMatches() {
        Match match = new Match();
        match.setId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
        match.setSurface(Surface.HARD);
        match.setStatus(MatchStatus.IN_PROGRESS);
        when(matchRepository.findByStatusOrderByScheduledAtDesc(
                        eq(MatchStatus.IN_PROGRESS), any(Pageable.class)))
                .thenReturn(List.of(match));

        var rows = matchService.listMatches(MatchStatus.IN_PROGRESS, null, 0, 20);

        assertEquals(1, rows.size());
        verify(matchRepository)
                .findByStatusOrderByScheduledAtDesc(
                        eq(MatchStatus.IN_PROGRESS), any(Pageable.class));
        verify(matchRepository, never())
                .findByStatusOrderByScheduledAtAsc(
                        eq(MatchStatus.IN_PROGRESS), any(Pageable.class));
    }

    @Test
    void tickerReturnsTheCachedAggregateWithoutCatalogueFanOut() {
        Match match = new Match();
        match.setId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
        match.setSurface(Surface.HARD);
        match.setStatus(MatchStatus.IN_PROGRESS);
        when(tickerCache.read()).thenReturn(Optional.of(List.of(MatchResponse.from(match))));

        var rows = matchService.listTicker();

        assertEquals(1, rows.size());
        verify(matchRepository, never()).findByStatusOrderByScheduledAtDesc(any(), any());
        verify(tickerCache, never()).write(any());
    }
}
