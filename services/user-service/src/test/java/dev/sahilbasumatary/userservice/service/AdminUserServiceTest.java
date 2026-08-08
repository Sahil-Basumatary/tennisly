package dev.sahilbasumatary.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.request.AdminUpdateUserRequest;
import dev.sahilbasumatary.userservice.entity.UserProfile;
import dev.sahilbasumatary.userservice.exception.UnauthorizedAccessException;
import dev.sahilbasumatary.userservice.repository.UserProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock private UserProfileRepository profileRepository;
    @InjectMocks private AdminUserService service;

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
    void listRequiresPlatformAdmin() {
        RequestContext.setRoles(Set.of());
        assertThrows(UnauthorizedAccessException.class, () -> service.list(null, null, 0, 20));
    }

    @Test
    void updateSoftDeactivatesUser() {
        UserProfile profile = sampleProfile(true);
        when(profileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);
        var response = service.update(USER_ID, new AdminUpdateUserRequest(null, false));
        assertEquals(false, response.active());
        verify(profileRepository).save(profile);
    }

    @Test
    void listReturnsPagedUsers() {
        UserProfile profile = sampleProfile(true);
        when(profileRepository.search(isNull(), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(profile)));
        var page = service.list(null, true, 0, 20);
        assertEquals(1, page.content().size());
        assertEquals("player@tennisly.dev", page.content().get(0).email());
    }

    private UserProfile sampleProfile(boolean active) {
        UserProfile profile = new UserProfile();
        profile.setId(USER_ID);
        profile.setClerkId("user_clerk_1");
        profile.setEmail("player@tennisly.dev");
        profile.setDisplayName("Ace Player");
        profile.setActive(active);
        profile.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        profile.setUpdatedAt(Instant.parse("2025-01-02T00:00:00Z"));
        return profile;
    }
}
