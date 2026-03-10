package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.userservice.dto.request.AddMemberRequest;
import dev.sahilbasumatary.userservice.dto.request.UpdateMemberRoleRequest;
import dev.sahilbasumatary.userservice.dto.request.UpdateOrganizationRequest;
import dev.sahilbasumatary.userservice.dto.response.MembershipResponse;
import dev.sahilbasumatary.userservice.dto.response.OrganizationResponse;
import dev.sahilbasumatary.userservice.service.OrganizationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/organizations")
public class OrganizationController {

    private static final Logger log = LoggerFactory.getLogger(OrganizationController.class);
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> listOrganizations() {
        log.debug("GET /api/users/organizations");
        return ResponseEntity.ok(organizationService.getCurrentUserOrganizations());
    }

    @GetMapping("/{orgId}")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID orgId) {
        log.debug("GET /api/users/organizations/{}", orgId);
        return ResponseEntity.ok(organizationService.getOrganization(orgId));
    }

    @PutMapping("/{orgId}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @PathVariable UUID orgId,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        log.debug("PUT /api/users/organizations/{}", orgId);
        return ResponseEntity.ok(organizationService.updateOrganization(orgId, request));
    }

    @GetMapping("/{orgId}/members")
    public ResponseEntity<List<MembershipResponse>> listMembers(@PathVariable UUID orgId) {
        log.debug("GET /api/users/organizations/{}/members", orgId);
        return ResponseEntity.ok(organizationService.listMembers(orgId));
    }

    @PostMapping("/{orgId}/members")
    public ResponseEntity<MembershipResponse> addMember(
            @PathVariable UUID orgId, @Valid @RequestBody AddMemberRequest request) {
        log.debug("POST /api/users/organizations/{}/members", orgId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.addMember(orgId, request));
    }

    @PutMapping("/{orgId}/members/{userId}")
    public ResponseEntity<MembershipResponse> updateMemberRole(
            @PathVariable UUID orgId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        log.debug("PUT /api/users/organizations/{}/members/{}", orgId, userId);
        return ResponseEntity.ok(
                organizationService.updateMemberRole(orgId, userId, request));
    }

    @DeleteMapping("/{orgId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID orgId, @PathVariable UUID userId) {
        log.debug("DELETE /api/users/organizations/{}/members/{}", orgId, userId);
        organizationService.removeMember(orgId, userId);
        return ResponseEntity.noContent().build();
    }
}
