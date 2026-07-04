package dev.sahilbasumatary.replayservice.repository;

import dev.sahilbasumatary.replayservice.entity.ReplayArtifact;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplayArtifactRepository extends JpaRepository<ReplayArtifact, UUID> {

    Optional<ReplayArtifact> findByMatchId(UUID matchId);

    boolean existsByMatchId(UUID matchId);

    void deleteByMatchId(UUID matchId);
}
