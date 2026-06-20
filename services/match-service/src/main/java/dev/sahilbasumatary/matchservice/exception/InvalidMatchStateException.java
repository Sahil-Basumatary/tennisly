package dev.sahilbasumatary.matchservice.exception;

public class InvalidMatchStateException extends RuntimeException {

    public InvalidMatchStateException(String message) {
        super(message);
    }
}
