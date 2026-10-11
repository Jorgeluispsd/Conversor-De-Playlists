package com.jorge.playlistconverter.errors.youtube;

public class YoutubeSearchException extends RuntimeException {
    public YoutubeSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
