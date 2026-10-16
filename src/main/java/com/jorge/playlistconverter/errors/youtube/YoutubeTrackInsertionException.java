package com.jorge.playlistconverter.errors.youtube;

public class YoutubeTrackInsertionException extends RuntimeException {
    public YoutubeTrackInsertionException(String message, Throwable cause){
        super(message, cause);
    }

    public YoutubeTrackInsertionException(String message) {
        super(message);
    }
}
