package dev.sahilbasumatary.matchservice.service;

import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchEventLog;
import dev.sahilbasumatary.matchservice.entity.MatchEventType;
import dev.sahilbasumatary.matchservice.repository.MatchEventLogRepository;
import dev.sahilbasumatary.matchservice.repository.MatchLiveSequenceStore;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MatchEventLogService {

    private final MatchEventLogRepository eventLogRepository;
    private final MatchLiveSequenceStore liveSequenceStore;

    public MatchEventLogService(
            MatchEventLogRepository eventLogRepository, MatchLiveSequenceStore liveSequenceStore) {
        this.eventLogRepository = eventLogRepository;
        this.liveSequenceStore = liveSequenceStore;
    }

    public long append(Match match, MatchEventType eventType, Map<String, Object> payload) {
        long sequence = liveSequenceStore.next(match.getId());
        MatchEventLog eventLog = new MatchEventLog();
        eventLog.setMatch(match);
        eventLog.setEventType(eventType);
        eventLog.setSequenceNumber(sequence);
        eventLog.setPayload(payload);
        eventLogRepository.save(eventLog);
        match.setLiveSequence(sequence);
        return sequence;
    }
}
