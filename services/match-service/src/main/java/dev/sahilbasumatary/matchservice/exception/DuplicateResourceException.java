package dev.sahilbasumatary.matchservice.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceName, Object value) {
        super(resourceName + " already exists: " + value);
    }
}
