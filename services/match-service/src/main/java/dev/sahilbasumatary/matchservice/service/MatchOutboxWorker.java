package dev.sahilbasumatary.matchservice.service;

import dev.sahilbasumatary.matchservice.client.MatchEventFanout;
import dev.sahilbasumatary.matchservice.entity.MatchOutboxEvent;
import dev.sahilbasumatary.matchservice.entity.MatchOutboxEvent.Status;
import dev.sahilbasumatary.matchservice.repository.MatchOutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchOutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(MatchOutboxWorker.class);
    private static final int BATCH = 25;
    private static final int MAX_ATTEMPTS = 8;

    private final MatchOutboxRepository outboxRepository;
    private final MatchEventFanout matchEventFanout;

    public MatchOutboxWorker(
            MatchOutboxRepository outboxRepository, MatchEventFanout matchEventFanout) {
        this.outboxRepository = outboxRepository;
        this.matchEventFanout = matchEventFanout;
    }

    @Scheduled(fixedDelayString = "${tennisly.outbox.poll-ms:2000}")
    @Transactional
    public void drain() {
        List<MatchOutboxEvent> batch =
                outboxRepository.findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                        Status.PENDING, Instant.now(), PageRequest.of(0, BATCH));
        for (MatchOutboxEvent row : batch) {
            try {
                matchEventFanout.relay(row.getEvent());
                row.setStatus(Status.SENT);
                row.setLastError(null);
            } catch (RuntimeException ex) {
                int attempts = row.getAttempts() + 1;
                row.setAttempts(attempts);
                row.setLastError(trim(ex.getMessage()));
                if (attempts >= MAX_ATTEMPTS) {
                    row.setStatus(Status.FAILED);
                    log.error("outbox exhausted id={} eventId={}", row.getId(), eventId(row), ex);
                } else {
                    long backoffSeconds = Math.min(300, 1L << Math.min(attempts, 8));
                    row.setAvailableAt(Instant.now().plus(Duration.ofSeconds(backoffSeconds)));
                    log.warn(
                            "outbox retry id={} attempts={} backoff={}s",
                            row.getId(),
                            attempts,
                            backoffSeconds);
                }
            }
        }
    }

    private static String eventId(MatchOutboxEvent row) {
        return row.getEvent() == null ? "?" : String.valueOf(row.getEvent().getEventId());
    }

    private static String trim(String message) {
        if (message == null) {
            return "error";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
