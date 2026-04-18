package dev.sahilbasumatary.tennisdataservice.repository;

import dev.sahilbasumatary.tennisdataservice.entity.Season;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, UUID> {

    Optional<Season> findByExternalId(String externalId);

    List<Season> findByTournamentId(UUID tournamentId);

    List<Season> findByYear(Integer year);
}
