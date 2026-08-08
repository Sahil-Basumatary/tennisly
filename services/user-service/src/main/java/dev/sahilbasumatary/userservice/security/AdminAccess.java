package dev.sahilbasumatary.userservice.security;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.exception.UnauthorizedAccessException;

public final class AdminAccess {

    private AdminAccess() {}

    public static void assertPlatformAdmin() {
        if (!RequestContext.hasRole("ADMIN")) {
            throw new UnauthorizedAccessException("Platform admin access required");
        }
    }
}
