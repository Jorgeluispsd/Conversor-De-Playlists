package com.jorge.playlistconverter.spotify.callback;

import com.sun.net.httpserver.HttpServer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class CallbackSession{

    private final HttpServer server;
    private final CompletableFuture<String> codeFuture;

    CallbackSession(HttpServer server, CompletableFuture<String> codeFuture){
        this.server = server;
        this.codeFuture = codeFuture;
    }

    public String awaitCode(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return codeFuture.get(timeout, unit);
    }

    public void stop(){
        server.stop(1);
    }
}
