package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.common.notification.EmailCategories;
import dev.sahilbasumatary.userservice.dto.response.EmailPreferenceResponse;
import dev.sahilbasumatary.userservice.dto.response.EmailRecipientResponse;
import dev.sahilbasumatary.userservice.service.UserPreferenceService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class InternalNotificationPreferenceController {

    private final UserPreferenceService preferenceService;

    public InternalNotificationPreferenceController(UserPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping("/users/by-clerk/{clerkId}/email-preference")
    public ResponseEntity<EmailPreferenceResponse> emailPreference(
            @PathVariable String clerkId, @RequestParam String category) {
        requireValidCategory(category);
        return ResponseEntity.ok(preferenceService.resolveEmailPreference(clerkId, category));
    }

    @GetMapping("/organizations/{organizationId}/email-recipients")
    public ResponseEntity<List<EmailRecipientResponse>> orgRecipients(
            @PathVariable UUID organizationId, @RequestParam String category) {
        requireValidCategory(category);
        return ResponseEntity.ok(preferenceService.listOrgEmailRecipients(organizationId, category));
    }

    private static void requireValidCategory(String category) {
        if (!EmailCategories.isValid(category)) {
            throw new IllegalArgumentException(
                    "Invalid email category: " + category + ". Valid: " + EmailCategories.all());
        }
    }
}
