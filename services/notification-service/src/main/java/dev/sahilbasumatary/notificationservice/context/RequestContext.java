package dev.sahilbasumatary.notificationservice.context;

import java.util.Collections;
import java.util.Set;

public final class RequestContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> ROLES = new ThreadLocal<>();

    private RequestContext() {}

    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void setRoles(Set<String> roles) {
        ROLES.set(roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet());
    }

    public static Set<String> getRoles() {
        Set<String> roles = ROLES.get();
        return roles != null ? roles : Collections.emptySet();
    }

    public static boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    public static void clear() {
        USER_ID.remove();
        ROLES.remove();
    }
}
