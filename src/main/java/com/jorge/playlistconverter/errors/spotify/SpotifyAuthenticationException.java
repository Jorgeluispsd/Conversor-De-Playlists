package com.jorge.playlistconverter.errors.spotify;

public class SpotifyAuthenticationException extends RuntimeException {
    public SpotifyAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
