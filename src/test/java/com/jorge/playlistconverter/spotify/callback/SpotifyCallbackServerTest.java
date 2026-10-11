package com.jorge.playlistconverter.spotify.callback;

import com.jorge.playlistconverter.errors.authorization.AuthorizationCallbackException;
import com.jorge.playlistconverter.errors.authorization.AuthorizationDeniedException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class SpotifyCallbackServerTest {

    private HttpExchange exchange;
    private HttpHandler handler;
    private CallbackSession session;

    @BeforeEach
    void setUp() throws IOException {
        HttpServer server = mock(HttpServer.class);
        exchange = mock(HttpExchange.class);

        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getResponseHeaders()).thenReturn(new Headers());

        doThrow(new IOException("Conexão simulada encerrada"))
                .when(exchange).sendResponseHeaders(anyInt(), anyLong());

        try(MockedStatic<HttpServer> serverFactory = mockStatic(HttpServer.class)) {

            serverFactory.when(() -> HttpServer.create(any(InetSocketAddress.class), eq(0)))
                    .thenReturn(server);

            session = new SpotifyCallbackServer().start("expected-state");
        }

        ArgumentCaptor<HttpHandler> handlerCaptor = ArgumentCaptor.forClass(HttpHandler.class);

        verify(server).createContext(eq("/callback"),(handlerCaptor.capture()));

        handler = handlerCaptor.getValue();
    }

    @AfterEach
    void shouldCloseExchange(){
        verify(exchange).close();
    }

    @Test
    void shouldPreserveCodeWhenHttpResponseFails() throws Exception{
        when(exchange.getRequestURI()).thenReturn(new URI(
                "/callback?state=expected-state&code=test-code"));

        handler.handle(exchange);

        String code = session.awaitCode(1, TimeUnit.SECONDS);

        assertEquals("test-code", code);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    void shouldPreserveDenialWhenHttpResponseFails() throws Exception{
        when(exchange.getRequestURI()).thenReturn(new URI(
                "/callback?state=expected-state&error=access_denied"));

        handler.handle(exchange);

        ExecutionException result = assertThrows(
                ExecutionException.class,
                () -> session.awaitCode(1, TimeUnit.SECONDS));

        AuthorizationDeniedException cause = assertInstanceOf(
                AuthorizationDeniedException.class,
                result.getCause());

        assertTrue(cause.getMessage().contains("access_denied"));

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

    @Test
    void shouldPreserveMissingCodeErrorWhenHttpResponseFails()
            throws Exception {

        when(exchange.getRequestURI()).thenReturn(URI.create(
                "/callback?state=expected-state"));

        handler.handle(exchange);

        ExecutionException result = assertThrows(
                ExecutionException.class,
                () -> session.awaitCode(1, TimeUnit.SECONDS));

        AuthorizationCallbackException cause = assertInstanceOf(
                AuthorizationCallbackException.class,
                result.getCause());

        assertEquals(
                "Callback sem código de autorização",
                cause.getMessage());

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

    @Test
    void shouldKeepWaitingAfterInvalidStateAndHttpResponseFailure()
            throws Exception {

        when(exchange.getRequestURI()).thenReturn(URI.create(
                "/callback?state=wrong-state&code=test-code"));

        handler.handle(exchange);

        assertThrows(
                TimeoutException.class,
                () -> session.awaitCode(1, TimeUnit.MILLISECONDS));

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

}
