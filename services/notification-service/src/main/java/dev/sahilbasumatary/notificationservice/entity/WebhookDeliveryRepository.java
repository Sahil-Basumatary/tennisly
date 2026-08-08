package dev.sahilbasumatary.notificationservice.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
