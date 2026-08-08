package dev.sahilbasumatary.analyticsservice.service;

import java.util.UUID;

public record ReindexJobStartedEvent(UUID jobId) {}
