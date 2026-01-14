package com.onepercentgrowth.local_to_smartapi.exceptions;

public class AuthExpiredException extends RuntimeException {

    public AuthExpiredException() {
        super("Authentication expired");
    }

    public AuthExpiredException(String message) {
        super(message);
    }

    public AuthExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}

