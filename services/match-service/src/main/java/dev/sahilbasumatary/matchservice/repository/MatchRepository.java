package dev.sahilbasumatary.matchservice.repository;

import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    @Query("""
        SELECT m FROM Match m
        WHERE m.status = :status
          AND (:cursor IS NULL OR m.id > :cursor)
        ORDER BY m.id ASC
        """)
    List<Match> findByStatusAfterCursor(
            @Param("status") MatchStatus status,
            @Param("cursor") UUID cursor,
            Pageable pageable);

    Optional<Match> findByExternalId(String externalId);

    List<Match> findAllByOrderByScheduledAtAsc();

    List<Match> findByStatusOrderByScheduledAtAsc(MatchStatus status);

    List<Match> findByTournamentIdOrderByScheduledAtAsc(UUID tournamentId);

    List<Match> findByTournamentIdAndStatusOrderByScheduledAtAsc(
            UUID tournamentId, MatchStatus status);
}
