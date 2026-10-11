package com.jorge.playlistconverter.errors.persistence;

public class SyncStateStorageException extends RuntimeException {
    public SyncStateStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
