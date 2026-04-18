package dev.sahilbasumatary.tennisdataservice.repository;

import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Player;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, UUID> {

    Optional<Player> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Player> findByGender(Gender gender);

    List<Player> findByNationality(String nationality);

    List<Player> findByActiveTrueOrderByCurrentRankingAsc();
}
