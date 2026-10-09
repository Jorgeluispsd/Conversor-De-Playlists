package com.jorge.playlistconverter.errors;

public class AuthorizationTimeoutException extends RuntimeException{

    public AuthorizationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
