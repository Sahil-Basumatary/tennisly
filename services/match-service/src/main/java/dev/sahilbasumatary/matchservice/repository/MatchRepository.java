package dev.sahilbasumatary.matchservice.repository;

import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    Optional<Match> findByExternalId(String externalId);

    List<Match> findAllByOrderByScheduledAtAsc();

    List<Match> findByStatusOrderByScheduledAtAsc(MatchStatus status);

    List<Match> findByTournamentIdOrderByScheduledAtAsc(UUID tournamentId);

    List<Match> findByTournamentIdAndStatusOrderByScheduledAtAsc(
            UUID tournamentId, MatchStatus status);
}
