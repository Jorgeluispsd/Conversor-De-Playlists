package com.jorge.playlistconverter.errors.youtube;

public class YoutubePlaylistCreationException extends RuntimeException {
    public YoutubePlaylistCreationException(String message) {
        super(message);
    }

    public YoutubePlaylistCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
