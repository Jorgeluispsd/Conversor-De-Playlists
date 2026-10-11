package com.jorge.playlistconverter.errors.authorization;

public class AuthorizationCallbackException extends RuntimeException{

    public AuthorizationCallbackException(String message) {
        super(message);
    }

    public AuthorizationCallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
