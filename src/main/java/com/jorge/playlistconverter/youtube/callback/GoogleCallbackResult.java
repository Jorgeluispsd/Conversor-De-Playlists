package com.jorge.playlistconverter.youtube.callback;

public record GoogleCallbackResult (
        GoogleCallbackStatus status,
        String code,
        String error
){

}


