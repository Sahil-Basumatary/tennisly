package dev.sahilbasumatary.replayservice.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(resourceName + " not found: " + resourceId);
    }
}
