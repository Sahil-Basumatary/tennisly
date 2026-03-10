package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.userservice.dto.request.UpdateProfileRequest;
import dev.sahilbasumatary.userservice.dto.response.UserProfileResponse;
import dev.sahilbasumatary.userservice.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);
    private final UserProfileService profileService;

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentProfile() {
        log.debug("GET /api/users/me");
        return ResponseEntity.ok(profileService.getOrCreateCurrentProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        log.debug("PUT /api/users/me");
        return ResponseEntity.ok(profileService.updateCurrentProfile(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getProfileById(@PathVariable UUID id) {
        log.debug("GET /api/users/{}", id);
        return ResponseEntity.ok(profileService.getProfileById(id));
    }
}
