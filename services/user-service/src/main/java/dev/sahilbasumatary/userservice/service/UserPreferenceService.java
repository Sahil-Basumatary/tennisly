package dev.sahilbasumatary.userservice.service;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.request.UpdatePreferencesRequest;
import dev.sahilbasumatary.userservice.dto.response.EmailPreferenceResponse;
import dev.sahilbasumatary.userservice.dto.response.EmailRecipientResponse;
import dev.sahilbasumatary.userservice.dto.response.PushPreferenceResponse;
import dev.sahilbasumatary.userservice.dto.response.PushRecipientResponse;
import dev.sahilbasumatary.userservice.dto.response.UserPreferenceResponse;
import dev.sahilbasumatary.userservice.entity.OrganizationMembership;
import dev.sahilbasumatary.userservice.entity.UserPreference;
import dev.sahilbasumatary.userservice.entity.UserProfile;
import dev.sahilbasumatary.userservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.userservice.notification.EmailPreferenceEvaluator;
import dev.sahilbasumatary.userservice.repository.OrganizationMembershipRepository;
import dev.sahilbasumatary.userservice.repository.UserPreferenceRepository;
import dev.sahilbasumatary.userservice.repository.UserProfileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(UserPreferenceService.class);
    private final UserPreferenceRepository preferenceRepository;
    private final UserProfileRepository profileRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public UserPreferenceService(
            UserPreferenceRepository preferenceRepository,
            UserProfileRepository profileRepository,
            OrganizationMembershipRepository membershipRepository) {
        this.preferenceRepository = preferenceRepository;
        this.profileRepository = profileRepository;
        this.membershipRepository = membershipRepository;
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

    @Transactional
    public UserPreference ensureDefaults(UserProfile profile) {
        return preferenceRepository
                .findByUserProfileId(profile.getId())
                .orElseGet(() -> createDefaults(profile));
    }

    @Transactional(readOnly = true)
    public EmailPreferenceResponse resolveEmailPreference(String clerkId, String category) {
        UserProfile profile =
                profileRepository
                        .findByClerkId(clerkId)
                        .orElseThrow(() -> new ResourceNotFoundException("UserProfile", clerkId));
        if (!profile.isActive() || profile.getEmail() == null || profile.getEmail().isBlank()) {
            return new EmailPreferenceResponse(
                    profile.getClerkId(), profile.getEmail(), displayName(profile), false);
        }
        UserPreference preference =
                preferenceRepository.findByUserProfileId(profile.getId()).orElse(null);
        boolean enabled =
                preference == null
                        || EmailPreferenceEvaluator.isCategoryEnabled(preference, category);
        return new EmailPreferenceResponse(
                profile.getClerkId(), profile.getEmail(), displayName(profile), enabled);
    }

    @Transactional(readOnly = true)
    public List<EmailRecipientResponse> listOrgEmailRecipients(
            UUID organizationId, String category) {
        List<EmailRecipientResponse> recipients = new ArrayList<>();
        for (OrganizationMembership membership :
                membershipRepository.findByOrganizationIdAndActiveTrue(organizationId)) {
            UserProfile profile = membership.getUserProfile();
            if (profile == null || !profile.isActive()) {
                continue;
            }
            if (profile.getEmail() == null || profile.getEmail().isBlank()) {
                continue;
            }
            UserPreference preference =
                    preferenceRepository.findByUserProfileId(profile.getId()).orElse(null);
            boolean enabled =
                    preference == null
                            || EmailPreferenceEvaluator.isCategoryEnabled(preference, category);
            if (!enabled) {
                continue;
            }
            recipients.add(
                    new EmailRecipientResponse(
                            profile.getClerkId(), profile.getEmail(), displayName(profile)));
        }
        return recipients;
    }

    @Transactional(readOnly = true)
    public PushPreferenceResponse resolvePushPreference(String clerkId, String category) {
        UserProfile profile =
                profileRepository
                        .findByClerkId(clerkId)
                        .orElseThrow(() -> new ResourceNotFoundException("UserProfile", clerkId));
        if (!profile.isActive()) {
            return new PushPreferenceResponse(profile.getClerkId(), displayName(profile), false);
        }
        UserPreference preference =
                preferenceRepository.findByUserProfileId(profile.getId()).orElse(null);
        boolean enabled =
                preference == null
                        || EmailPreferenceEvaluator.isPushCategoryEnabled(preference, category);
        return new PushPreferenceResponse(profile.getClerkId(), displayName(profile), enabled);
    }

    @Transactional(readOnly = true)
    public List<PushRecipientResponse> listOrgPushRecipients(UUID organizationId, String category) {
        List<PushRecipientResponse> recipients = new ArrayList<>();
        for (OrganizationMembership membership :
                membershipRepository.findByOrganizationIdAndActiveTrue(organizationId)) {
            UserProfile profile = membership.getUserProfile();
            if (profile == null || !profile.isActive()) {
                continue;
            }
            UserPreference preference =
                    preferenceRepository.findByUserProfileId(profile.getId()).orElse(null);
            boolean enabled =
                    preference == null
                            || EmailPreferenceEvaluator.isPushCategoryEnabled(preference, category);
            if (!enabled) {
                continue;
            }
            recipients.add(new PushRecipientResponse(profile.getClerkId(), displayName(profile)));
        }
        return recipients;
    }

    private UserPreference createDefaults(UserProfile profile) {
        UserPreference preference = new UserPreference();
        preference.setUserProfile(profile);
        preference.setExtraSettings(EmailPreferenceEvaluator.seededExtraSettings());
        preferenceRepository.save(preference);
        log.info("Created default preferences for userId={}", profile.getId());
        return preference;
    }

    private void applyUpdates(UserPreference preference, UpdatePreferencesRequest request) {
        if (request.theme() != null) {
            preference.setTheme(request.theme());
        }
        if (request.notificationsEnabled() != null) {
            preference.setNotificationsEnabled(request.notificationsEnabled());
        }
        if (request.emailNotifications() != null) {
            preference.setEmailNotifications(request.emailNotifications());
        }
        if (request.pushNotifications() != null) {
            preference.setPushNotifications(request.pushNotifications());
        }
        if (request.favoriteSurface() != null) {
            preference.setFavoriteSurface(request.favoriteSurface());
        }
        if (request.locale() != null) {
            preference.setLocale(request.locale());
        }
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

    private static String displayName(UserProfile profile) {
        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }
        String first = profile.getFirstName() == null ? "" : profile.getFirstName().trim();
        String last = profile.getLastName() == null ? "" : profile.getLastName().trim();
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? profile.getEmail() : combined;
    }
}
