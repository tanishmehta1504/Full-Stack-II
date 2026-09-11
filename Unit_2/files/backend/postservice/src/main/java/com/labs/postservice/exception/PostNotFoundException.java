package com.labs.postservice.exception;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(Long id) {
        super("post " + id + " was not found");
    }
}
