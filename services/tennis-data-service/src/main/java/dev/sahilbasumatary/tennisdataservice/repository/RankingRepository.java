package dev.sahilbasumatary.tennisdataservice.repository;

import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Ranking;
import dev.sahilbasumatary.tennisdataservice.entity.RankingType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingRepository extends JpaRepository<Ranking, UUID> {

    List<Ranking> findByPlayerId(UUID playerId);

    List<Ranking> findByRankingDateAndRankingTypeAndGenderOrderByRankAsc(
            LocalDate rankingDate, RankingType rankingType, Gender gender);

    List<Ranking> findByPlayerIdAndRankingTypeOrderByRankingDateDesc(
            UUID playerId, RankingType rankingType);
}
