package dev.sahilbasumatary.notificationservice.client.dto;

public record EmailPreferenceResponse(
        String clerkId, String email, String displayName, boolean enabled) {}
