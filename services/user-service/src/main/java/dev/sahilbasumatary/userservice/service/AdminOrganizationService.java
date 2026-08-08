package dev.sahilbasumatary.userservice.service;

import dev.sahilbasumatary.userservice.dto.request.AdminUpdateOrganizationRequest;
import dev.sahilbasumatary.userservice.dto.response.AdminOrganizationResponse;
import dev.sahilbasumatary.userservice.dto.response.AdminPageResponse;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.userservice.repository.OrganizationRepository;
import dev.sahilbasumatary.userservice.security.AdminAccess;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOrganizationService {

    private static final Logger log = LoggerFactory.getLogger(AdminOrganizationService.class);
    private final OrganizationRepository organizationRepository;

    public AdminOrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminOrganizationResponse> list(
            String q, Boolean active, int page, int size) {
        AdminAccess.assertPlatformAdmin();
        String query = normalizeQuery(q);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<AdminOrganizationResponse> result =
                organizationRepository.search(query, active, pageable).map(AdminOrganizationResponse::from);
        return AdminPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public AdminOrganizationResponse get(UUID id) {
        AdminAccess.assertPlatformAdmin();
        Organization org =
                organizationRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Organization", id));
        return AdminOrganizationResponse.from(org);
    }

    @Transactional
    public AdminOrganizationResponse update(UUID id, AdminUpdateOrganizationRequest request) {
        AdminAccess.assertPlatformAdmin();
        Organization org =
                organizationRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Organization", id));
        if (request.name() != null) org.setName(request.name());
        if (request.description() != null) org.setDescription(request.description());
        if (request.logoUrl() != null) org.setLogoUrl(request.logoUrl());
        if (request.website() != null) org.setWebsite(request.website());
        if (request.planTier() != null) org.setPlanTier(request.planTier());
        if (request.maxMembers() != null) org.setMaxMembers(request.maxMembers());
        if (request.active() != null) org.setActive(request.active());
        organizationRepository.save(org);
        log.info("Admin updated organization orgId={}", id);
        return AdminOrganizationResponse.from(org);
    }

    private String normalizeQuery(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim();
    }
}
