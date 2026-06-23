package com.rozkladjazdy.jazdaz.exceptions;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Void> handleResourceNotFound(
            ResourceNotFoundException exception
    ) {

        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(BadDataException.class)
    public ResponseEntity<Void> handleBadData(
            BadDataException exception
    ) {

        return ResponseEntity.badRequest().build();
    }
}
