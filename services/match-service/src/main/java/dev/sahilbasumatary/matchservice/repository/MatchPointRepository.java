package dev.sahilbasumatary.matchservice.repository;

import dev.sahilbasumatary.matchservice.entity.MatchPoint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchPointRepository extends JpaRepository<MatchPoint, UUID> {

    int countByMatchId(UUID matchId);

    List<MatchPoint> findByMatchIdOrderBySequenceNumberAsc(UUID matchId);
}
