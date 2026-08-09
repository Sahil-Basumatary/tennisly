package dev.sahilbasumatary.notificationservice.push;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushDeliveryRepository extends JpaRepository<PushDelivery, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);
}
