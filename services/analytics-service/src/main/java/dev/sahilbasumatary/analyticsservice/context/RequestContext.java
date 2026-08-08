package dev.sahilbasumatary.analyticsservice.context;

public final class RequestContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ORG_ID = new ThreadLocal<>();

    private RequestContext() {}

    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void setOrgId(String orgId) {
        ORG_ID.set(orgId);
    }

    public static String getOrgId() {
        return ORG_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
        ORG_ID.remove();
    }
}
