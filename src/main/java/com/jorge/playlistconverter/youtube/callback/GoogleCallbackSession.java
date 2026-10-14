package com.jorge.playlistconverter.youtube.callback;

import com.sun.net.httpserver.HttpServer;
import lombok.Getter;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GoogleCallbackSession {

    private final HttpServer server;
    private final CompletableFuture<String> codeFuture;

    @Getter
    private final String redirectUri;

    GoogleCallbackSession(HttpServer server, CompletableFuture<String> codeFuture,
                          String redirectUri) {
        this.server = Objects.requireNonNull(server);
        this.codeFuture = Objects.requireNonNull(codeFuture);
        this.redirectUri = Objects.requireNonNull(redirectUri);
    }

    public String awaitCode(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {

        return codeFuture.get(timeout, unit);
    }

    public void stop(){
        server.stop(1);
    }
}
