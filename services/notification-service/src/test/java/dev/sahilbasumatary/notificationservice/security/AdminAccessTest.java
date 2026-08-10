package dev.sahilbasumatary.notificationservice.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sahilbasumatary.notificationservice.context.RequestContext;
import dev.sahilbasumatary.notificationservice.exception.UnauthorizedAccessException;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AdminAccessTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void rejectsWhenNotPlatformAdmin() {
        RequestContext.setUserId("user_abc");
        assertThrows(UnauthorizedAccessException.class, AdminAccess::assertPlatformAdmin);
    }

    @Test
    void allowsPlatformAdminRole() {
        RequestContext.setUserId("admin_abc");
        RequestContext.setRoles(Set.of("ADMIN"));
        assertDoesNotThrow(AdminAccess::assertPlatformAdmin);
    }
}
