package com.jorge.playlistconverter.errors.spotify;

public class SpotifyPlaylistReadException extends RuntimeException {
    public SpotifyPlaylistReadException(String message, Throwable cause) {

        super(message, cause);
    }
}
