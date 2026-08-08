package dev.sahilbasumatary.userservice.repository;

import dev.sahilbasumatary.userservice.entity.AuditLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query(
            """
            SELECT a FROM AuditLog a
            WHERE (:q IS NULL OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.actorClerkId) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.resourceId) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:action IS NULL OR a.action = :action)
            AND (:organizationId IS NULL OR a.organizationId = :organizationId)
            AND (:from IS NULL OR a.createdAt >= :from)
            AND (:to IS NULL OR a.createdAt <= :to)
            """)
    Page<AuditLog> search(
            @Param("q") String q,
            @Param("action") String action,
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
