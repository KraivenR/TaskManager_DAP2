package com.kraiven.taskmanager.exception;

/** Signals that a task is missing or hidden because another user owns it. */
public class TaskNotFoundException extends RuntimeException {
 
    public TaskNotFoundException(Long id) {
        super("Task not found with id: " + id);
    }
}
