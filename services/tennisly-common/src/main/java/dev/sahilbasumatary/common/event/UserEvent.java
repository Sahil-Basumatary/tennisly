package dev.sahilbasumatary.common.event;

public class UserEvent extends BaseEvent {

    private String clerkId;
    private String email;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private boolean active;

    public UserEvent() {}

    public UserEvent(String eventType, String clerkId) {
        super(eventType, "auth-service");
        this.clerkId = clerkId;
    }

    public static UserEvent created(String clerkId, String email,
            String firstName, String lastName, String imageUrl) {
        UserEvent event = new UserEvent("USER_CREATED", clerkId);
        event.email = email;
        event.firstName = firstName;
        event.lastName = lastName;
        event.imageUrl = imageUrl;
        event.active = true;
        return event;
    }

    public static UserEvent updated(String clerkId, String email,
            String firstName, String lastName, String imageUrl) {
        UserEvent event = new UserEvent("USER_UPDATED", clerkId);
        event.email = email;
        event.firstName = firstName;
        event.lastName = lastName;
        event.imageUrl = imageUrl;
        event.active = true;
        return event;
    }

    public static UserEvent deleted(String clerkId) {
        UserEvent event = new UserEvent("USER_DELETED", clerkId);
        event.active = false;
        return event;
    }

    public String getClerkId() {
        return clerkId;
    }

    public void setClerkId(String clerkId) {
        this.clerkId = clerkId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
