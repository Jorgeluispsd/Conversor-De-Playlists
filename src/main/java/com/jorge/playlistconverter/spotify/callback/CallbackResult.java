package com.jorge.playlistconverter.spotify.callback;

public record CallbackResult(CallbackStatus status, String code, String error) {
}
