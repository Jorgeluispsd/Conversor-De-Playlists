package com.jorge.playlistconverter.errors.authorization;

public class CallbackServerStartupException extends RuntimeException {
    public CallbackServerStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}
