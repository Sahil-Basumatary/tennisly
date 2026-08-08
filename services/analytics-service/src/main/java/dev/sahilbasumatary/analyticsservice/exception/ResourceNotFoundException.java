package dev.sahilbasumatary.analyticsservice.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(resourceName + " not found: " + resourceId);
    }
}
