package dev.sahilbasumatary.matchservice.dto.request;

import dev.sahilbasumatary.matchservice.entity.PlayerSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateMatchPlayerRequest(
        @NotNull UUID playerId,
        @NotBlank @Size(max = 255) String displayName,
        @NotNull PlayerSide side,
        Integer seedNumber) {}
