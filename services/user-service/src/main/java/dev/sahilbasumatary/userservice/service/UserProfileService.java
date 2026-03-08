package dev.sahilbasumatary.userservice.service;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.request.UpdateProfileRequest;
import dev.sahilbasumatary.userservice.dto.response.UserProfileResponse;
import dev.sahilbasumatary.userservice.entity.OrganizationMembership;
import dev.sahilbasumatary.userservice.entity.UserProfile;
import dev.sahilbasumatary.userservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.userservice.exception.UnauthorizedAccessException;
import dev.sahilbasumatary.userservice.repository.OrganizationMembershipRepository;
import dev.sahilbasumatary.userservice.repository.UserProfileRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private final UserProfileRepository profileRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public UserProfileService(
            UserProfileRepository profileRepository,
            OrganizationMembershipRepository membershipRepository) {
        this.profileRepository = profileRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public UserProfileResponse getOrCreateCurrentProfile() {
        String clerkId = RequestContext.getUserId();
        UserProfile profile =
                profileRepository.findByClerkId(clerkId).orElseGet(() -> createDefault(clerkId));
        return UserProfileResponse.from(profile);
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(UpdateProfileRequest request) {
        String clerkId = RequestContext.getUserId();
        UserProfile profile =
                profileRepository
                        .findByClerkId(clerkId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("UserProfile", clerkId));
        applyUpdates(profile, request);
        profileRepository.save(profile);
        log.info("Updated profile for clerkId={}", clerkId);
        return UserProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(UUID id) {
        UserProfile profile =
                profileRepository
                        .findById(id)
                        .filter(UserProfile::isActive)
                        .orElseThrow(() -> new ResourceNotFoundException("UserProfile", id));
        if (RequestContext.hasRole("ADMIN")) {
            return UserProfileResponse.from(profile);
        }
        String currentClerkId = RequestContext.getUserId();
        UserProfile currentUser =
                profileRepository
                        .findByClerkId(currentClerkId)
                        .filter(UserProfile::isActive)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("UserProfile", currentClerkId));
        if (currentUser.getId().equals(profile.getId())) {
            return UserProfileResponse.from(profile);
        }
        if (!sharesOrganization(currentUser.getId(), profile.getId())) {
            throw new UnauthorizedAccessException(
                    "You do not have permission to view this profile");
        }
        return UserProfileResponse.from(profile);
    }

    private boolean sharesOrganization(UUID userIdA, UUID userIdB) {
        Set<UUID> orgIdsA =
                membershipRepository.findByUserProfileIdAndActiveTrue(userIdA).stream()
                        .map(m -> m.getOrganization().getId())
                        .collect(Collectors.toSet());
        if (orgIdsA.isEmpty()) {
            return false;
        }
        List<OrganizationMembership> membershipsB =
                membershipRepository.findByUserProfileIdAndActiveTrue(userIdB);
        return membershipsB.stream()
                .anyMatch(m -> orgIdsA.contains(m.getOrganization().getId()));
    }

    private UserProfile createDefault(String clerkId) {
        UserProfile profile = new UserProfile();
        profile.setClerkId(clerkId);
        // Placeholder until the user updates their profile via PUT /me
        profile.setEmail(clerkId + "@pending.tennisly.dev");
        profile.setActive(true);
        profileRepository.save(profile);
        log.info("Lazily created profile for clerkId={}", clerkId);
        return profile;
    }

    private void applyUpdates(UserProfile profile, UpdateProfileRequest request) {
        if (request.displayName() != null) profile.setDisplayName(request.displayName());
        if (request.firstName() != null) profile.setFirstName(request.firstName());
        if (request.lastName() != null) profile.setLastName(request.lastName());
        if (request.email() != null) profile.setEmail(request.email());
        if (request.phone() != null) profile.setPhone(request.phone());
        if (request.country() != null) profile.setCountry(request.country());
        if (request.timezone() != null) profile.setTimezone(request.timezone());
        if (request.bio() != null) profile.setBio(request.bio());
        if (request.avatarUrl() != null) profile.setAvatarUrl(request.avatarUrl());
        if (request.skillLevel() != null) profile.setSkillLevel(request.skillLevel());
    }
}
