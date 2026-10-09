package com.jorge.playlistconverter.errors;

public class SpotifyAuthenticationException extends RuntimeException {
    public SpotifyAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
