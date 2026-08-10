package dev.sahilbasumatary.matchservice.repository;

import dev.sahilbasumatary.matchservice.entity.MatchPoint;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchPointRepository extends JpaRepository<MatchPoint, UUID> {

    int countByMatchId(UUID matchId);

    @Query(
            """
            select p.match.id, count(p)
            from MatchPoint p
            where p.match.id in :ids
            group by p.match.id
            """)
    List<Object[]> countGroupedByMatchIds(@Param("ids") Collection<UUID> ids);

    List<MatchPoint> findByMatchIdOrderBySequenceNumberAsc(UUID matchId);

    void deleteByMatchId(UUID matchId);
}
