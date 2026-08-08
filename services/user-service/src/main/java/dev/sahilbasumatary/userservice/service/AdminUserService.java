package dev.sahilbasumatary.userservice.service;

import dev.sahilbasumatary.userservice.dto.request.AdminUpdateUserRequest;
import dev.sahilbasumatary.userservice.dto.response.AdminPageResponse;
import dev.sahilbasumatary.userservice.dto.response.AdminUserResponse;
import dev.sahilbasumatary.userservice.entity.UserProfile;
import dev.sahilbasumatary.userservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.userservice.repository.UserProfileRepository;
import dev.sahilbasumatary.userservice.security.AdminAccess;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);
    private final UserProfileRepository profileRepository;
    private final AuditLogService auditLogService;

    public AdminUserService(UserProfileRepository profileRepository, AuditLogService auditLogService) {
        this.profileRepository = profileRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminUserResponse> list(String q, Boolean active, int page, int size) {
        AdminAccess.assertPlatformAdmin();
        String query = normalizeQuery(q);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("email").ascending());
        Page<AdminUserResponse> result =
                profileRepository.search(query, active, pageable).map(AdminUserResponse::from);
        return AdminPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID id) {
        AdminAccess.assertPlatformAdmin();
        UserProfile profile =
                profileRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("UserProfile", id));
        return AdminUserResponse.from(profile);
    }

    @Transactional
    public AdminUserResponse update(UUID id, AdminUpdateUserRequest request) {
        AdminAccess.assertPlatformAdmin();
        UserProfile profile =
                profileRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("UserProfile", id));
        if (request.displayName() != null) profile.setDisplayName(request.displayName());
        if (request.active() != null) profile.setActive(request.active());
        profileRepository.save(profile);
        Map<String, Object> metadata = new HashMap<>();
        if (request.displayName() != null) metadata.put("displayName", request.displayName());
        if (request.active() != null) metadata.put("active", request.active());
        String action =
                request.active() != null && !request.active() ? "USER_DEACTIVATE" : "USER_UPDATE";
        auditLogService.record(action, "USER", id.toString(), null, metadata);
        log.info("Admin updated user profile userId={}", id);
        return AdminUserResponse.from(profile);
    }

    private String normalizeQuery(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim();
    }
}
