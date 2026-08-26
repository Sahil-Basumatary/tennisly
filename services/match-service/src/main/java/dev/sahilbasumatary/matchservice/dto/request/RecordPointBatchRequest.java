package dev.sahilbasumatary.matchservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RecordPointBatchRequest(
        String idempotencyKey,
        @NotEmpty @Size(max = 1000) List<@Valid RecordPointRequest> points) {}
