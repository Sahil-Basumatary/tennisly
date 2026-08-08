package dev.sahilbasumatary.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.entity.AuditLog;
import dev.sahilbasumatary.userservice.entity.UserProfile;
import dev.sahilbasumatary.userservice.exception.UnauthorizedAccessException;
import dev.sahilbasumatary.userservice.repository.AuditLogRepository;
import dev.sahilbasumatary.userservice.repository.UserProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    private static final UUID ORG_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @InjectMocks private AuditLogService service;

    @BeforeEach
    void setUp() {
        RequestContext.setUserId("admin_clerk");
        RequestContext.setRoles(Set.of("ADMIN"));
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void recordPersistsActorFromRequestContext() {
        UserProfile profile = new UserProfile();
        profile.setEmail("admin@tennisly.dev");
        when(userProfileRepository.findByClerkId("admin_clerk")).thenReturn(Optional.of(profile));
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service.record("ORG_UPDATE", "ORGANIZATION", ORG_ID.toString(), ORG_ID, Map.of("name", "Club"));
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("admin_clerk", saved.getActorClerkId());
        assertEquals("admin@tennisly.dev", saved.getActorEmail());
        assertEquals("ORG_UPDATE", saved.getAction());
        assertEquals("Club", saved.getMetadata().get("name"));
    }

    @Test
    void listRequiresPlatformAdmin() {
        RequestContext.setRoles(Set.of("MEMBER"));
        assertThrows(
                UnauthorizedAccessException.class,
                () -> service.list(null, null, null, null, null, 0, 20));
    }

    @Test
    void listAppliesFilters() {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setActorClerkId("admin_clerk");
        log.setAction("API_KEY_CREATE");
        log.setResourceType("API_KEY");
        log.setOrganizationId(ORG_ID);
        log.setMetadata(Map.of());
        log.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        when(auditLogRepository.search(
                        isNull(),
                        eq("API_KEY_CREATE"),
                        eq(ORG_ID),
                        isNull(),
                        isNull(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));
        var page = service.list(null, "API_KEY_CREATE", ORG_ID, null, null, 0, 20);
        assertEquals(1, page.content().size());
        assertEquals("API_KEY_CREATE", page.content().get(0).action());
        verify(auditLogRepository)
                .search(isNull(), eq("API_KEY_CREATE"), eq(ORG_ID), isNull(), isNull(), any(Pageable.class));
    }
}
