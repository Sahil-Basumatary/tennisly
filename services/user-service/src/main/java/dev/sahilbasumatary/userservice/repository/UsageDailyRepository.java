package dev.sahilbasumatary.userservice.repository;

import dev.sahilbasumatary.userservice.entity.UsageDaily;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageDailyRepository extends JpaRepository<UsageDaily, UUID> {

    Optional<UsageDaily> findByOrganizationIdAndMetricAndDay(
            UUID organizationId, String metric, LocalDate day);

    @Query(
            """
            SELECT u FROM UsageDaily u
            WHERE u.organization.id = :organizationId
            AND (:from IS NULL OR u.day >= :from)
            AND (:to IS NULL OR u.day <= :to)
            ORDER BY u.day DESC, u.metric ASC
            """)
    List<UsageDaily> findByOrganizationAndRange(
            @Param("organizationId") UUID organizationId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
