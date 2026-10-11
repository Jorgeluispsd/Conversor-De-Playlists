package com.jorge.playlistconverter.errors.persistence;

public class DatabaseInitializationException extends RuntimeException {
    public DatabaseInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
