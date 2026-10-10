package com.jorge.playlistconverter.errors;

public class OperationInterruptedException extends RuntimeException {
    public OperationInterruptedException(String message, Throwable cause) {

        super(message, cause);
    }
}
