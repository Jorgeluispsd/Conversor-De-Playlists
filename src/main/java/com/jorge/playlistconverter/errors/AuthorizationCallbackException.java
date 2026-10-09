package com.jorge.playlistconverter.errors;

public class AuthorizationCallbackException extends RuntimeException{

    public AuthorizationCallbackException(String message) {
        super(message);
    }

    public AuthorizationCallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
