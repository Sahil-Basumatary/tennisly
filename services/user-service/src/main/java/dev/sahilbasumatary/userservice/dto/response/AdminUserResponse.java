package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.UserProfile;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String clerkId,
        String email,
        String displayName,
        String firstName,
        String lastName,
        String phone,
        String country,
        String timezone,
        String bio,
        String avatarUrl,
        String skillLevel,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminUserResponse from(UserProfile profile) {
        return new AdminUserResponse(
                profile.getId(),
                profile.getClerkId(),
                profile.getEmail(),
                profile.getDisplayName(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhone(),
                profile.getCountry(),
                profile.getTimezone(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getSkillLevel(),
                profile.isActive(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
