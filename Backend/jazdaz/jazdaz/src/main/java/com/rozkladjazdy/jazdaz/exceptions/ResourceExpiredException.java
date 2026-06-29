package com.rozkladjazdy.jazdaz.exceptions;

public class ResourceExpiredException extends RuntimeException {
    public ResourceExpiredException(String message) {
        super(message);
    }
    public ResourceExpiredException() {
        super();
    }
}
