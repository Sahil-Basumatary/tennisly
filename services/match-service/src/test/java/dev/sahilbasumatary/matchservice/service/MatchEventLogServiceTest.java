package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchEventLog;
import dev.sahilbasumatary.matchservice.entity.MatchEventType;
import dev.sahilbasumatary.matchservice.repository.MatchEventLogRepository;
import dev.sahilbasumatary.matchservice.repository.MatchLiveSequenceStore;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MatchEventLogServiceTest {

    @Test
    void allocatesAndPersistsTheNextMatchSequence() {
        MatchEventLogRepository eventLogRepository = mock(MatchEventLogRepository.class);
        MatchLiveSequenceStore liveSequenceStore = mock(MatchLiveSequenceStore.class);
        MatchEventLogService service =
                new MatchEventLogService(eventLogRepository, liveSequenceStore);
        UUID matchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Match match = new Match();
        match.setId(matchId);
        when(liveSequenceStore.next(matchId)).thenReturn(42L);

        long sequence =
                service.append(match, MatchEventType.POINT_RECORDED, Map.of("score", "15-0"));

        ArgumentCaptor<MatchEventLog> eventCaptor = ArgumentCaptor.forClass(MatchEventLog.class);
        verify(eventLogRepository).save(eventCaptor.capture());
        assertEquals(42, sequence);
        assertEquals(42, match.getLiveSequence());
        assertEquals(42, eventCaptor.getValue().getSequenceNumber());
        assertEquals(match, eventCaptor.getValue().getMatch());
    }
}
