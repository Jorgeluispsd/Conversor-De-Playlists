package com.jorge.playlistconverter.errors;

public class SpotifyPlaylistReadException extends RuntimeException {
    public SpotifyPlaylistReadException(String message, Throwable cause) {

        super(message, cause);
    }
}
