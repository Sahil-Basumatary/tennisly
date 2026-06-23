package dev.sahilbasumatary.matchservice.service;

import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchEventLog;
import dev.sahilbasumatary.matchservice.entity.MatchEventType;
import dev.sahilbasumatary.matchservice.repository.MatchEventLogRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MatchEventLogService {

    private final MatchEventLogRepository eventLogRepository;

    public MatchEventLogService(MatchEventLogRepository eventLogRepository) {
        this.eventLogRepository = eventLogRepository;
    }

    public void append(Match match, MatchEventType eventType, Map<String, Object> payload) {
        MatchEventLog eventLog = new MatchEventLog();
        eventLog.setMatch(match);
        eventLog.setEventType(eventType);
        eventLog.setPayload(payload);
        eventLogRepository.save(eventLog);
    }
}
