package dev.sahilbasumatary.userservice.dto.response;

public record EmailPreferenceResponse(
        String clerkId, String email, String displayName, boolean enabled) {}
