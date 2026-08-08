package dev.sahilbasumatary.analyticsservice.repository;

import dev.sahilbasumatary.analyticsservice.entity.SavedAnalyticsView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedAnalyticsViewRepository extends JpaRepository<SavedAnalyticsView, UUID> {

    List<SavedAnalyticsView> findByUserIdOrderByFavoriteDescUpdatedAtDesc(String userId);

    Optional<SavedAnalyticsView> findByIdAndUserId(UUID id, String userId);

    long countByUserId(String userId);
}
