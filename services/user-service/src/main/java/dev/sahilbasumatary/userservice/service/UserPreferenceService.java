package dev.sahilbasumatary.userservice.service;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.request.UpdatePreferencesRequest;
import dev.sahilbasumatary.userservice.dto.response.UserPreferenceResponse;
import dev.sahilbasumatary.userservice.entity.UserPreference;
import dev.sahilbasumatary.userservice.entity.UserProfile;
import dev.sahilbasumatary.userservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.userservice.repository.UserPreferenceRepository;
import dev.sahilbasumatary.userservice.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(UserPreferenceService.class);
    private final UserPreferenceRepository preferenceRepository;
    private final UserProfileRepository profileRepository;

    public UserPreferenceService(
            UserPreferenceRepository preferenceRepository,
            UserProfileRepository profileRepository) {
        this.preferenceRepository = preferenceRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public UserPreferenceResponse getOrCreatePreferences() {
        UserProfile profile = resolveCurrentProfile();
        UserPreference preference =
                preferenceRepository
                        .findByUserProfileId(profile.getId())
                        .orElseGet(() -> createDefaults(profile));
        return UserPreferenceResponse.from(preference);
    }

    @Transactional
    public UserPreferenceResponse updatePreferences(UpdatePreferencesRequest request) {
        UserProfile profile = resolveCurrentProfile();
        UserPreference preference =
                preferenceRepository
                        .findByUserProfileId(profile.getId())
                        .orElseGet(() -> createDefaults(profile));
        applyUpdates(preference, request);
        preferenceRepository.save(preference);
        log.info("Updated preferences for userId={}", profile.getId());
        return UserPreferenceResponse.from(preference);
    }

    private UserPreference createDefaults(UserProfile profile) {
        UserPreference preference = new UserPreference();
        preference.setUserProfile(profile);
        preferenceRepository.save(preference);
        log.info("Created default preferences for userId={}", profile.getId());
        return preference;
    }

    private void applyUpdates(UserPreference preference, UpdatePreferencesRequest request) {
        if (request.theme() != null) preference.setTheme(request.theme());
        if (request.notificationsEnabled() != null) {
            preference.setNotificationsEnabled(request.notificationsEnabled());
        }
        if (request.emailNotifications() != null) {
            preference.setEmailNotifications(request.emailNotifications());
        }
        if (request.favoriteSurface() != null) {
            preference.setFavoriteSurface(request.favoriteSurface());
        }
        if (request.locale() != null) preference.setLocale(request.locale());
        if (request.extraSettings() != null) {
            preference.setExtraSettings(request.extraSettings());
        }
    }

    private UserProfile resolveCurrentProfile() {
        String clerkId = RequestContext.getUserId();
        return profileRepository
                .findByClerkId(clerkId)
                .filter(UserProfile::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", clerkId));
    }
}
