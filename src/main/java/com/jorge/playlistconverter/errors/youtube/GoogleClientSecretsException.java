package com.jorge.playlistconverter.errors.youtube;

public class GoogleClientSecretsException extends RuntimeException {
    public GoogleClientSecretsException(String message) {
        super(message);
    }

    public GoogleClientSecretsException(String message, Throwable cause) {
        super(message, cause);
    }
}
