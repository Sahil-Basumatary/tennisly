package dev.sahilbasumatary.analyticsservice.repository;

import dev.sahilbasumatary.analyticsservice.entity.AnalyticsReindexJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsReindexJobRepository extends JpaRepository<AnalyticsReindexJob, UUID> {}
