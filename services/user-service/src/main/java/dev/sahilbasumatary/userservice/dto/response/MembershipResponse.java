package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.MemberRole;
import dev.sahilbasumatary.userservice.entity.OrganizationMembership;
import java.time.Instant;
import java.util.UUID;

public record MembershipResponse(
        UUID id,
        UUID userId,
        String displayName,
        String email,
        MemberRole role,
        Instant joinedAt) {

    public static MembershipResponse from(OrganizationMembership membership) {
        return new MembershipResponse(
                membership.getId(),
                membership.getUserProfile().getId(),
                membership.getUserProfile().getDisplayName(),
                membership.getUserProfile().getEmail(),
                membership.getRole(),
                membership.getJoinedAt());
    }
}
