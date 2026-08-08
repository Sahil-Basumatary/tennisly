package dev.sahilbasumatary.notificationservice.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    @Query("""
            SELECT d FROM WebhookDelivery d
            WHERE d.status IN ('PENDING','FAILED')
              AND d.nextAttemptAt <= :now
              AND d.attemptCount < d.maxAttempts
            ORDER BY d.nextAttemptAt
            LIMIT :limit
            """)
    List<WebhookDelivery> findDueDeliveries(Instant now, int limit);

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT d FROM WebhookDelivery d
            WHERE (:organizationId IS NULL OR d.organizationId = :organizationId)
              AND (:endpointId IS NULL OR d.endpointId = :endpointId)
              AND (:status IS NULL OR d.status = :status)
              AND (:eventType IS NULL OR d.eventType = :eventType)
            """)
    Page<WebhookDelivery> search(
            @Param("organizationId") UUID organizationId,
            @Param("endpointId") UUID endpointId,
            @Param("status") DeliveryStatus status,
            @Param("eventType") String eventType,
            Pageable pageable);
}
