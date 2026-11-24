package com.kraiven.taskmanager.exception;

/** Signals that a requested user or category does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
}
