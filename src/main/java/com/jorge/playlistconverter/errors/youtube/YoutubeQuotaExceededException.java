package com.jorge.playlistconverter.errors.youtube;

public class YoutubeQuotaExceededException extends RuntimeException {
    public YoutubeQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
