package dev.sahilbasumatary.userservice.dto.request;

import dev.sahilbasumatary.userservice.entity.MemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull MemberRole role) {}
