package com.kraiven.taskmanager.exception;

/** Signals that a request conflicts with an existing record or relationship. */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
