package com.jorge.playlistconverter.errors.spotify;

public class SpotifyRequestLimitException extends RuntimeException {
    private final Integer retryAfterSeconds;

    public SpotifyRequestLimitException(String message, Integer retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
