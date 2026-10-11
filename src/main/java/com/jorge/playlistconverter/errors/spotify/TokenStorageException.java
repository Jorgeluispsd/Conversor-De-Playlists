package com.jorge.playlistconverter.errors.spotify;

public class TokenStorageException extends RuntimeException{

    public TokenStorageException(String message, Throwable cause){
        super(message, cause);
    }
}
