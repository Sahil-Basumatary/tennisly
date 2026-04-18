package dev.sahilbasumatary.tennisdataservice.repository;

import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Surface;
import dev.sahilbasumatary.tennisdataservice.entity.Tournament;
import dev.sahilbasumatary.tennisdataservice.entity.TournamentLevel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

    Optional<Tournament> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Tournament> findByLevel(TournamentLevel level);

    List<Tournament> findBySurface(Surface surface);

    List<Tournament> findByGender(Gender gender);
}
