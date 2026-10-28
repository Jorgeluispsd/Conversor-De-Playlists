package com.jorge.playlistconverter.errors.input;

public class InvalidPlaylistInputException extends IllegalArgumentException {
    public InvalidPlaylistInputException(String message) {
        super(message);
    }

    public InvalidPlaylistInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
