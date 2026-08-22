package dev.sahilbasumatary.matchservice.repository;

import dev.sahilbasumatary.matchservice.entity.MatchOutboxEvent;
import dev.sahilbasumatary.matchservice.entity.MatchOutboxEvent.Status;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchOutboxRepository extends JpaRepository<MatchOutboxEvent, UUID> {

    List<MatchOutboxEvent> findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            Status status, Instant availableAt, Pageable pageable);
}
