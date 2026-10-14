package com.jorge.playlistconverter.youtube.callback;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class GoogleCallbackServerTest {

    private HttpServer server;
    private HttpExchange exchange;
    private HttpHandler handler;
    private GoogleCallbackSession session;


    @BeforeEach
    void setUp() throws IOException {
        server = mock(HttpServer.class);
        exchange = mock(HttpExchange.class);

        when(server.getAddress()).thenReturn(
                new InetSocketAddress("127.0.0.1", 49152));

        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getResponseBody()).thenReturn(
                new ByteArrayOutputStream());

        try (MockedStatic<HttpServer> serverFactory =
                     mockStatic(HttpServer.class)) {

            serverFactory.when(() -> HttpServer.create(
                    any(InetSocketAddress.class), eq(0))).thenReturn(server);

            session = new GoogleCallbackServer().start("expected-state");
        }

        ArgumentCaptor<HttpHandler> handlerCaptor =
                ArgumentCaptor.forClass(HttpHandler.class);

        verify(server).createContext(eq("/callback"), handlerCaptor.capture());

        handler = handlerCaptor.getValue();
    }

    @AfterEach
    void shouldCloseExchange() {
        verify(exchange, atLeastOnce()).close();
    }

    @Test
    void shouldReceiveCodeAndExposeRedirectUri() throws Exception {
        handle("/callback?state=expected-state&code=test-code");

        assertEquals(
                "test-code", session.awaitCode(1, TimeUnit.SECONDS));

        assertEquals(
                "http://127.0.0.1:49152/callback", session.getRedirectUri());

        verify(exchange).sendResponseHeaders(eq(200), anyLong());

        session.stop();

        verify(server).stop(1);
    }

    @Test
    void shouldReportAuthorizationDenied() throws Exception {
        handle("/callback?state=expected-state&error=access_denied");

        ExecutionException result = assertThrows(
                ExecutionException.class,
                () -> session.awaitCode(1, TimeUnit.SECONDS));

        assertInstanceOf(
                AuthorizationDeniedException.class,
                result.getCause());

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

    @Test
    void shouldReportMissingCode() throws Exception {
        handle("/callback?state=expected-state");

        ExecutionException result = assertThrows(
                ExecutionException.class,
                () -> session.awaitCode(1, TimeUnit.SECONDS));

        AuthorizationCallbackException cause = assertInstanceOf(
                AuthorizationCallbackException.class,
                result.getCause());

        assertEquals(
                "Callback Google sem código de autorização.",
                cause.getMessage());

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

    @Test
    void shouldIgnoreInvalidStateAndAcceptLaterValidCallback()
            throws Exception {

        handle("/callback?state=wrong-state&code=wrong-code");

        assertSessionIsPending();

        verify(exchange).sendResponseHeaders(eq(400), anyLong());

        handle("/callback?state=expected-state&code=correct-code");

        assertEquals(
                "correct-code", session.awaitCode(1, TimeUnit.SECONDS));

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        verify(exchange, times(2)).close();
    }

    @Test
    void shouldRejectInvalidMethodWithoutCompletingSession()
            throws Exception {

        when(exchange.getRequestMethod()).thenReturn("POST");

        handle("/callback?state=expected-state&code=test-code");

        assertSessionIsPending();

        verify(exchange).sendResponseHeaders(eq(405), anyLong());
    }

    @Test
    void shouldRejectInvalidPathWithoutCompletingSession()
            throws Exception {

        handle("/callback/extra?state=expected-state&code=test-code");

        assertSessionIsPending();

        verify(exchange).sendResponseHeaders(eq(404), anyLong());
    }

    @Test
    void shouldPreserveCodeWhenHttpResponseFails() throws Exception {
        doThrow(new IOException("Conexão simulada encerrada"))
                .when(exchange)
                .sendResponseHeaders(anyInt(), anyLong());

        handle("/callback?state=expected-state&code=test-code");

        assertEquals(
                "test-code", session.awaitCode(1, TimeUnit.SECONDS));

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }

    private void handle(String uri) throws IOException {
        when(exchange.getRequestURI()).thenReturn(URI.create(uri));

        handler.handle(exchange);
    }

    private void assertSessionIsPending() {
        assertThrows(
                TimeoutException.class,
                () -> session.awaitCode(1, TimeUnit.MILLISECONDS));
    }
}
