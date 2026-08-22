package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.kafka.EventPublisher;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.matchservice.dto.response.MatchLiveEventResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.Surface;
import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class MatchEventDispatchTest {

    private final UUID matchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final MatchRealtimeNotifier realtimeNotifier = mock(MatchRealtimeNotifier.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final MatchOutboxWriter outboxWriter = mock(MatchOutboxWriter.class);
    private final Executor directExecutor = command -> Objects.requireNonNull(command).run();
    private final MatchTimers matchTimers = new MatchTimers(new SimpleMeterRegistry());
    private final MatchEventDispatch dispatch =
            new MatchEventDispatch(
                    realtimeNotifier, eventPublisher, outboxWriter, directExecutor, matchTimers);

    @Test
    void persistsOutboxBeforeImmediateFanoutWithoutTransaction() {
        MatchEvent event = MatchEvent.created(matchId, "SCHEDULED");
        event.setSequence(1);
        MatchResponse response = response();

        dispatch.publish(matchId, event, response);

        InOrder order = inOrder(outboxWriter, realtimeNotifier, eventPublisher);
        order.verify(outboxWriter).enqueue(event);
        order.verify(realtimeNotifier).publish(any(MatchLiveEventResponse.class));
        order.verify(eventPublisher)
                .publish(TopicNames.MATCH_EVENTS, matchId.toString(), event);
        ArgumentCaptor<MatchLiveEventResponse> liveEventCaptor =
                ArgumentCaptor.forClass(MatchLiveEventResponse.class);
        verify(realtimeNotifier).publish(liveEventCaptor.capture());
        MatchLiveEventResponse liveEvent = liveEventCaptor.getValue();
        assertEquals(1, liveEvent.sequence());
        assertEquals(event.getEventId(), liveEvent.eventId());
        assertEquals(response, liveEvent.snapshot());
        assertNotNull(liveEvent.commitObservedAt());
    }

    @Test
    void defersRealtimeFanoutUntilAfterCommit() {
        withSynchronization(
                () -> {
                    MatchEvent event = MatchEvent.created(matchId, "SCHEDULED");
                    MatchResponse response = response();
                    dispatch.publish(matchId, event, response);
                    verify(outboxWriter).enqueue(event);
                    verifyNoInteractions(realtimeNotifier, eventPublisher);
                    for (TransactionSynchronization synchronization :
                            TransactionSynchronizationManager.getSynchronizations()) {
                        synchronization.afterCommit();
                    }
                    verify(realtimeNotifier).publish(any(MatchLiveEventResponse.class));
                    verify(eventPublisher)
                            .publish(TopicNames.MATCH_EVENTS, matchId.toString(), event);
                });
    }

    @Test
    void doesNotFanoutWhenTransactionNeverCommits() {
        withSynchronization(
                () -> {
                    MatchEvent event = MatchEvent.created(matchId, "SCHEDULED");
                    dispatch.publish(matchId, event, response());
                    verify(outboxWriter).enqueue(event);
                    verifyNoInteractions(realtimeNotifier, eventPublisher);
                });
    }

    @Test
    void postCommitFanoutFailureCannotRollBackDurableEvent() {
        MatchEvent event = MatchEvent.created(matchId, "SCHEDULED");
        MatchResponse response = response();
        doThrow(new IllegalStateException("redis unavailable"))
                .when(realtimeNotifier)
                .publish(any(MatchLiveEventResponse.class));

        assertDoesNotThrow(() -> dispatch.publish(matchId, event, response));

        verify(outboxWriter).enqueue(event);
    }

    private void withSynchronization(Runnable action) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            action.run();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private MatchResponse response() {
        return new MatchResponse(
                matchId,
                "perf-match",
                null,
                Surface.HARD,
                MatchStatus.SCHEDULED,
                3,
                null,
                null,
                null,
                Map.of(),
                Map.of(),
                List.of(),
                0,
                0,
                null,
                null);
    }
}
