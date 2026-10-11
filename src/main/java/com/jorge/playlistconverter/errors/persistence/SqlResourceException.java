package com.jorge.playlistconverter.errors.persistence;

public class SqlResourceException extends RuntimeException {

    public SqlResourceException(String message) {
        super(message);
    }

    public SqlResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
