package dev.sahilbasumatary.matchservice.service;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.matchservice.entity.MatchOutboxEvent;
import dev.sahilbasumatary.matchservice.repository.MatchOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchOutboxWriter {

    private final MatchOutboxRepository outboxRepository;

    public MatchOutboxWriter(MatchOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public void enqueue(MatchEvent event) {
        MatchOutboxEvent row = new MatchOutboxEvent();
        row.setEvent(event);
        outboxRepository.save(row);
    }
}
