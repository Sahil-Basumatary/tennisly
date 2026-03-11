package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.userservice.dto.request.UpdatePreferencesRequest;
import dev.sahilbasumatary.userservice.dto.response.UserPreferenceResponse;
import dev.sahilbasumatary.userservice.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/preferences")
public class UserPreferenceController {

    private static final Logger log = LoggerFactory.getLogger(UserPreferenceController.class);
    private final UserPreferenceService preferenceService;

    public UserPreferenceController(UserPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<UserPreferenceResponse> getPreferences() {
        log.debug("GET /api/users/me/preferences");
        return ResponseEntity.ok(preferenceService.getOrCreatePreferences());
    }

    @PutMapping
    public ResponseEntity<UserPreferenceResponse> updatePreferences(
            @Valid @RequestBody UpdatePreferencesRequest request) {
        log.debug("PUT /api/users/me/preferences");
        return ResponseEntity.ok(preferenceService.updatePreferences(request));
    }
}
