package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.UserProfile;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
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
        Instant createdAt,
        Instant updatedAt) {

    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
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
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
