package com.kraiven.taskmanager.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;


/** Converts common application exceptions into consistent HTTP error responses. */
@ControllerAdvice 
public class GlobalControllerAdvice {

    /** Returns validation errors and their field names as a 400 response. */
    @ExceptionHandler (MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>
            handleTaskNotFoundException(MethodArgumentNotValidException ex, WebRequest request) {
                
                Map<String, Object> body = new HashMap<>();
                body.put("timestamp", LocalDateTime.now());
                body.put("message", "Invalid Input");
                

               List<String> errors = ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> error.getField() + ":" + error.getDefaultMessage())
                        .collect(Collectors.toList());

                body.put("errors", errors);
                return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
                           
            }
    
    /** Returns 404 when a task is missing or inaccessible to the caller. */
    @ExceptionHandler (TaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>>
            handleTaskNotFoundException(TaskNotFoundException ex, WebRequest request) {

                Map<String, Object> body = new HashMap<>();
                body.put("timestamp", LocalDateTime.now());
                body.put("message", ex.getMessage());
                body.put("path", request.getDescription(false)
                        .replace("uri", ""));

                        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
                           
            }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /** Returns 409 when an operation conflicts with existing data or relationships. */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleResourceConflictException(
            ResourceConflictException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

              @ExceptionHandler (Exception.class)
    public ResponseEntity<Map<String, Object>>
            handleTaskNotFoundException(Exception ex, WebRequest request) {

                Map<String, Object> body = new HashMap<>();
                body.put("timestamp", LocalDateTime.now());
                body.put("message", "An Unexpected Error Occured");
                body.put("details", ex.getMessage());

                        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
                           
            }  
}
