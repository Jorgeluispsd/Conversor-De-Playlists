package com.jorge.playlistconverter.errors;

public class YoutubePlaylistReadException extends RuntimeException {
    public YoutubePlaylistReadException(String message, Throwable cause) {

        super(message, cause);
    }
}
