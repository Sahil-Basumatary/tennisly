package dev.sahilbasumatary.matchservice.repository;

import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByStatusOrderByIdAsc(MatchStatus status, Pageable pageable);

    List<Match> findByStatusAndIdGreaterThanOrderByIdAsc(
            MatchStatus status, UUID cursor, Pageable pageable);

    @EntityGraph(attributePaths = "players")
    Optional<Match> findByExternalId(String externalId);

    @EntityGraph(attributePaths = "players")
    @Override
    Optional<Match> findById(UUID id);

    @EntityGraph(attributePaths = "players")
    List<Match> findAllByOrderByScheduledAtAsc(Pageable pageable);

    @EntityGraph(attributePaths = "players")
    List<Match> findByStatusOrderByScheduledAtAsc(MatchStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "players")
    List<Match> findByStatusOrderByScheduledAtDesc(MatchStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "players")
    List<Match> findByTournamentIdOrderByScheduledAtAsc(UUID tournamentId, Pageable pageable);

    @EntityGraph(attributePaths = "players")
    List<Match> findByTournamentIdAndStatusOrderByScheduledAtAsc(
            UUID tournamentId, MatchStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "players")
    List<Match> findByTournamentIdAndStatusOrderByScheduledAtDesc(
            UUID tournamentId, MatchStatus status, Pageable pageable);
}
