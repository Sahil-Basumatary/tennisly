package dev.sahilbasumatary.userservice.service;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.request.AddMemberRequest;
import dev.sahilbasumatary.userservice.dto.request.UpdateMemberRoleRequest;
import dev.sahilbasumatary.userservice.dto.request.UpdateOrganizationRequest;
import dev.sahilbasumatary.userservice.dto.response.MembershipResponse;
import dev.sahilbasumatary.userservice.dto.response.OrganizationResponse;
import dev.sahilbasumatary.userservice.entity.MemberRole;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.OrganizationMembership;
import dev.sahilbasumatary.userservice.entity.UserProfile;
import dev.sahilbasumatary.userservice.exception.DuplicateResourceException;
import dev.sahilbasumatary.userservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.userservice.exception.UnauthorizedAccessException;
import dev.sahilbasumatary.userservice.repository.OrganizationMembershipRepository;
import dev.sahilbasumatary.userservice.repository.OrganizationRepository;
import dev.sahilbasumatary.userservice.repository.UserProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserProfileRepository profileRepository;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            UserProfileRepository profileRepository) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getCurrentUserOrganizations() {
        UserProfile profile = resolveCurrentProfile();
        return membershipRepository.findByUserProfileIdAndActiveTrue(profile.getId()).stream()
                .map(m -> OrganizationResponse.from(m.getOrganization()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(UUID orgId) {
        Organization org = findActiveOrg(orgId);
        if (!RequestContext.hasRole("ADMIN")) {
            assertMembership(orgId);
        }
        return OrganizationResponse.from(org);
    }

    @Transactional
    public OrganizationResponse updateOrganization(
            UUID orgId, UpdateOrganizationRequest request) {
        Organization org = findActiveOrg(orgId);
        assertOrgAdmin(orgId);
        if (request.name() != null) org.setName(request.name());
        if (request.description() != null) org.setDescription(request.description());
        if (request.logoUrl() != null) org.setLogoUrl(request.logoUrl());
        if (request.website() != null) org.setWebsite(request.website());
        organizationRepository.save(org);
        log.info("Updated organization orgId={}", orgId);
        return OrganizationResponse.from(org);
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> listMembers(UUID orgId) {
        findActiveOrg(orgId);
        if (!RequestContext.hasRole("ADMIN")) {
            assertMembership(orgId);
        }
        return membershipRepository.findByOrganizationIdAndActiveTrue(orgId).stream()
                .map(MembershipResponse::from)
                .toList();
    }

    @Transactional
    public MembershipResponse addMember(UUID orgId, AddMemberRequest request) {
        Organization org = findActiveOrg(orgId);
        assertOrgAdmin(orgId);
        UserProfile targetProfile =
                profileRepository
                        .findById(request.userId())
                        .filter(UserProfile::isActive)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "UserProfile", request.userId()));
        if (membershipRepository.existsByUserProfileIdAndOrganizationId(
                targetProfile.getId(), orgId)) {
            throw new DuplicateResourceException(
                    "Membership", targetProfile.getId() + " in org " + orgId);
        }
        long currentCount = membershipRepository.countByOrganizationIdAndActiveTrue(orgId);
        if (currentCount >= org.getMaxMembers()) {
            throw new IllegalStateException(
                    "Organization has reached its maximum member limit of "
                            + org.getMaxMembers());
        }
        OrganizationMembership membership = new OrganizationMembership();
        membership.setUserProfile(targetProfile);
        membership.setOrganization(org);
        membership.setRole(request.role());
        membership.setJoinedAt(Instant.now());
        membership.setActive(true);
        membershipRepository.save(membership);
        log.info(
                "Added member userId={} to orgId={} with role={}",
                request.userId(),
                orgId,
                request.role());
        return MembershipResponse.from(membership);
    }

    @Transactional
    public MembershipResponse updateMemberRole(
            UUID orgId, UUID userId, UpdateMemberRoleRequest request) {
        findActiveOrg(orgId);
        assertOrgAdmin(orgId);
        OrganizationMembership membership =
                membershipRepository
                        .findByUserProfileIdAndOrganizationId(userId, orgId)
                        .filter(OrganizationMembership::isActive)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Membership", userId + " in org " + orgId));
        if (membership.getRole() == MemberRole.OWNER && request.role() != MemberRole.OWNER) {
            long ownerCount =
                    membershipRepository.findByOrganizationIdAndActiveTrue(orgId).stream()
                            .filter(m -> m.getRole() == MemberRole.OWNER)
                            .count();
            if (ownerCount <= 1) {
                throw new IllegalStateException("Cannot change role of the last owner");
            }
        }
        membership.setRole(request.role());
        membershipRepository.save(membership);
        log.info("Updated role for userId={} in orgId={} to {}", userId, orgId, request.role());
        return MembershipResponse.from(membership);
    }

    @Transactional
    public void removeMember(UUID orgId, UUID userId) {
        findActiveOrg(orgId);
        assertOrgAdmin(orgId);
        OrganizationMembership membership =
                membershipRepository
                        .findByUserProfileIdAndOrganizationId(userId, orgId)
                        .filter(OrganizationMembership::isActive)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Membership", userId + " in org " + orgId));
        if (membership.getRole() == MemberRole.OWNER) {
            long ownerCount =
                    membershipRepository.findByOrganizationIdAndActiveTrue(orgId).stream()
                            .filter(m -> m.getRole() == MemberRole.OWNER)
                            .count();
            if (ownerCount <= 1) {
                throw new IllegalStateException(
                        "Cannot remove the last owner of an organization");
            }
        }
        membership.setActive(false);
        membershipRepository.save(membership);
        log.info("Removed member userId={} from orgId={}", userId, orgId);
    }

    private Organization findActiveOrg(UUID orgId) {
        return organizationRepository
                .findById(orgId)
                .filter(Organization::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));
    }

    private UserProfile resolveCurrentProfile() {
        String clerkId = RequestContext.getUserId();
        return profileRepository
                .findByClerkId(clerkId)
                .filter(UserProfile::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", clerkId));
    }

    private void assertMembership(UUID orgId) {
        UserProfile profile = resolveCurrentProfile();
        boolean isMember =
                membershipRepository
                        .findByUserProfileIdAndOrganizationId(profile.getId(), orgId)
                        .filter(OrganizationMembership::isActive)
                        .isPresent();
        if (!isMember) {
            throw new UnauthorizedAccessException(
                    "You are not a member of this organization");
        }
    }

    private void assertOrgAdmin(UUID orgId) {
        if (RequestContext.hasRole("ADMIN")) return;
        UserProfile profile = resolveCurrentProfile();
        OrganizationMembership membership =
                membershipRepository
                        .findByUserProfileIdAndOrganizationId(profile.getId(), orgId)
                        .filter(OrganizationMembership::isActive)
                        .orElseThrow(
                                () -> new UnauthorizedAccessException(
                                        "You are not a member of this organization"));
        if (membership.getRole() != MemberRole.OWNER
                && membership.getRole() != MemberRole.ADMIN) {
            throw new UnauthorizedAccessException(
                    "You must be an owner or admin to perform this action");
        }
    }
}
