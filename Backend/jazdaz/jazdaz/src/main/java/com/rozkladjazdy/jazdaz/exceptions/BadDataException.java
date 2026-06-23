package com.rozkladjazdy.jazdaz.exceptions;

public class BadDataException extends RuntimeException{
    public BadDataException(String message) {
        super(message);
    }
    public BadDataException() {
        super();
    }
}
