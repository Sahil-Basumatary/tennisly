package dev.sahilbasumatary.analyticsservice.repository;

import dev.sahilbasumatary.analyticsservice.entity.AnalyticsIngestReceipt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsIngestReceiptRepository
        extends JpaRepository<AnalyticsIngestReceipt, UUID> {

    boolean existsByEventId(String eventId);

    Optional<AnalyticsIngestReceipt> findByEventId(String eventId);
}
