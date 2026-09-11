package com.labs.postservice.exception;

public class InvalidPostStateException extends RuntimeException {
    public InvalidPostStateException(String message) {
        super(message);
    }
}
