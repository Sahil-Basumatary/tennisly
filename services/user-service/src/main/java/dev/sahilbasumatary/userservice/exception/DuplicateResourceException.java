package dev.sahilbasumatary.userservice.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceType, Object identifier) {
        super(resourceType + " already exists with identifier: " + identifier);
    }
}
