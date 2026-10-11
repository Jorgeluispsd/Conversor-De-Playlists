package com.jorge.playlistconverter.errors.authorization;

public class AuthorizationTimeoutException extends RuntimeException{

    public AuthorizationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
