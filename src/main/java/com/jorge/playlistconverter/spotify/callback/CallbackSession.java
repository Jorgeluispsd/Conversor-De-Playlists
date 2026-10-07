package com.jorge.playlistconverter.spotify.callback;

import com.sun.net.httpserver.HttpServer;

import java.util.concurrent.CompletableFuture;

public record CallbackSession(HttpServer server,
                              CompletableFuture<String> codeFuture) {
}
