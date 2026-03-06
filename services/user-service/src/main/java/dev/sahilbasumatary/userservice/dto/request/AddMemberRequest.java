package dev.sahilbasumatary.userservice.dto.request;

import dev.sahilbasumatary.userservice.entity.MemberRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddMemberRequest(
        @NotNull UUID userId,
        @NotNull MemberRole role) {}
