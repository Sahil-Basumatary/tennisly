package dev.sahilbasumatary.notificationservice.security;

import dev.sahilbasumatary.notificationservice.context.RequestContext;
import dev.sahilbasumatary.notificationservice.exception.UnauthorizedAccessException;

public final class AdminAccess {

    private AdminAccess() {}

    public static void assertPlatformAdmin() {
        if (!RequestContext.hasRole("ADMIN")) {
            throw new UnauthorizedAccessException("Platform admin access required");
        }
    }
}
