package dev.sahilbasumatary.analyticsservice.repository;

import dev.sahilbasumatary.analyticsservice.entity.SavedAnalyticsView;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedAnalyticsViewRepository extends JpaRepository<SavedAnalyticsView, UUID> {

    List<SavedAnalyticsView> findByUserIdOrderByUpdatedAtDesc(String userId);
}
