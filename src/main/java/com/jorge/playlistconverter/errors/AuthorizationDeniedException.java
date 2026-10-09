package com.jorge.playlistconverter.errors;

public class AuthorizationDeniedException extends RuntimeException{

    public AuthorizationDeniedException(String message) {
        super(message);
    }
}
