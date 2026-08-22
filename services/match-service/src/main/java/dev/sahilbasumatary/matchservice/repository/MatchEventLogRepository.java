package dev.sahilbasumatary.matchservice.repository;

import dev.sahilbasumatary.matchservice.entity.MatchEventLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchEventLogRepository extends JpaRepository<MatchEventLog, UUID> {

    List<MatchEventLog> findByMatchIdOrderByCreatedAtAsc(UUID matchId);

    List<MatchEventLog> findByMatchIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
            UUID matchId, long sequenceNumber, Pageable pageable);
}
