package com.jorge.playlistconverter.errors;

public class YoutubeSearchException extends RuntimeException {
    public YoutubeSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
