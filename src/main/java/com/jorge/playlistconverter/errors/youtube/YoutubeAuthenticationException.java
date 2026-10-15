package com.jorge.playlistconverter.errors.youtube;

public class YoutubeAuthenticationException extends RuntimeException {
    public YoutubeAuthenticationException(String message, Throwable cause) {

        super(message, cause);
    }

    public YoutubeAuthenticationException(String message) {
        super(message);
    }
}
