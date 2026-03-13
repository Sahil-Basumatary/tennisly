package dev.sahilbasumatary.common.event;

public class OrganizationEvent extends BaseEvent {

    private String clerkOrgId;
    private String name;
    private String slug;
    private String imageUrl;
    private boolean active;

    public OrganizationEvent() {}

    public OrganizationEvent(String eventType, String clerkOrgId) {
        super(eventType, "auth-service");
        this.clerkOrgId = clerkOrgId;
    }

    public static OrganizationEvent created(String clerkOrgId, String name,
            String slug, String imageUrl) {
        OrganizationEvent event =
                new OrganizationEvent("ORGANIZATION_CREATED", clerkOrgId);
        event.name = name;
        event.slug = slug;
        event.imageUrl = imageUrl;
        event.active = true;
        return event;
    }

    public static OrganizationEvent updated(String clerkOrgId, String name,
            String slug, String imageUrl) {
        OrganizationEvent event =
                new OrganizationEvent("ORGANIZATION_UPDATED", clerkOrgId);
        event.name = name;
        event.slug = slug;
        event.imageUrl = imageUrl;
        event.active = true;
        return event;
    }

    public static OrganizationEvent deleted(String clerkOrgId) {
        OrganizationEvent event =
                new OrganizationEvent("ORGANIZATION_DELETED", clerkOrgId);
        event.active = false;
        return event;
    }

    public String getClerkOrgId() {
        return clerkOrgId;
    }

    public void setClerkOrgId(String clerkOrgId) {
        this.clerkOrgId = clerkOrgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
