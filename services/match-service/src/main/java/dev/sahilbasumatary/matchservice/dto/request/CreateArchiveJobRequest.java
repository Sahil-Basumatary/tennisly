package dev.sahilbasumatary.matchservice.dto.request;

public record CreateArchiveJobRequest(
        String idempotencyKey, Long expectedRows, Long expectedBytes, String expectedSha256) {}
