package com.jorge.playlistconverter.errors.authorization;

public class AuthorizationDeniedException extends RuntimeException{

    public AuthorizationDeniedException(String message) {
        super(message);
    }
}
