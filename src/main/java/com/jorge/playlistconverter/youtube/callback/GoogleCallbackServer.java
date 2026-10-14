package com.jorge.playlistconverter.youtube.callback;

import com.jorge.playlistconverter.errors.authorization.AuthorizationCallbackException;
import com.jorge.playlistconverter.errors.authorization.AuthorizationDeniedException;
import com.jorge.playlistconverter.errors.authorization.CallbackServerStartupException;
import static com.jorge.playlistconverter.commons.http.QueryParameterParser.extractParam;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class GoogleCallbackServer {

    private static final String CALLBACK_PATH = "/callback";

    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;

    public GoogleCallbackSession start(String expectedState) {
        Objects.requireNonNull(expectedState);

        if (expectedState.isBlank()){
            throw new IllegalArgumentException(
                    "O state da autorização Google não pode estar vazio.");
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        HttpServer server;

        try {
            server = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", 0),0);

        }catch (IOException e){
            throw new CallbackServerStartupException(
                    "Não foi possível abrir o servidor callback Google.", e);
        }

        try {
            server.createContext(CALLBACK_PATH, exchange -> handleCallback(
                    exchange, expectedState, future));

            server.start();

            String redirectUri = "http://127.0.0.1:"
                    + server.getAddress().getPort()
                    + CALLBACK_PATH;

            return new GoogleCallbackSession(server, future, redirectUri);

        } catch (RuntimeException e){
            server.stop(0);

            throw new CallbackServerStartupException(
                    "Não foi possível iniciar o servidor callback Google.", e);
        }
    }

    private GoogleCallbackResult classifyCallback(HttpExchange exchange, String expectedState){
        String path = exchange.getRequestURI().getPath();

        if (!CALLBACK_PATH.equals(path)){
            return new GoogleCallbackResult(
                    GoogleCallbackStatus.INVALID_PATH, null, null);
        }

        if (!"GET".equals(exchange.getRequestMethod())){
            return new GoogleCallbackResult(
                    GoogleCallbackStatus.INVALID_METHOD, null, null);
        }

        String query = exchange.getRequestURI().getRawQuery();

        String receivedState;
        String error;
        String code;

        try {
            receivedState = extractParam(query, "state");
            error = extractParam(query, "error");
            code = extractParam(query, "code");

        }catch (IllegalArgumentException e){
            return new GoogleCallbackResult(
                    GoogleCallbackStatus.INVALID_PARAMETERS, null, null);
        }

        GoogleCallbackStatus status = switch (receivedState){
            case null ->
                    GoogleCallbackStatus.INVALID_STATE;

            case String state when !expectedState.equals(state) ->
                    GoogleCallbackStatus.INVALID_STATE;

            case String state when error != null ->
                    GoogleCallbackStatus.AUTHORIZATION_DENIED;

            case String state when code == null || code.isBlank() ->
                    GoogleCallbackStatus.MISSING_CODE;

            default ->
                    GoogleCallbackStatus.SUCCESS;
        };

        return new GoogleCallbackResult(status, code, error);
    }

    private void handleCallback(HttpExchange exchange, String expectedState,
                                CompletableFuture<String> future){
        try {
            GoogleCallbackResult result =
                    classifyCallback(exchange, expectedState);

            switch (result.status()){
                case INVALID_PATH -> sendResponse(
                        exchange, HTTP_NOT_FOUND, "Caminho não encontrado");

                case INVALID_METHOD -> sendResponse(
                        exchange, HTTP_METHOD_NOT_ALLOWED, "Método não permitido");

                case INVALID_PARAMETERS -> sendResponse(
                        exchange, HTTP_BAD_REQUEST, "Parâmetros inválidos");

                case INVALID_STATE -> {
                    log.warn("Callback Google recebido com state inválido");

                    sendResponse(exchange, HTTP_BAD_REQUEST,
                            "Retorno inválido: state não corresponde ao login iniciado.");
                }

                case AUTHORIZATION_DENIED -> {
                    future.completeExceptionally(
                            new AuthorizationDeniedException(
                                    "A autorização Google não foi concedida."));

                    sendResponse(exchange, HTTP_BAD_REQUEST,
                            "Autorização não concluída. Volte ao terminal.");
                }

                case MISSING_CODE -> {
                    future.completeExceptionally(
                            new AuthorizationCallbackException(
                                    "Callback Google sem código de autorização."));

                    sendResponse(exchange, HTTP_BAD_REQUEST,
                            "Código de autorização ausente.");
                }

                case SUCCESS -> {
                    future.complete(result.code());

                    sendResponse(exchange, HTTP_OK,
                            "Autorização recebida. Volte ao terminal.");
                }
            }

        }catch (IOException e){
            log.warn("Não foi possível enviar a resposta HTTP do callback Google. Tipo: {}",
                    e.getClass().getSimpleName());

        }catch (RuntimeException e){
            future.completeExceptionally(
                    new AuthorizationCallbackException(
                            "Falha ao processar o callback Google.", e));

        }finally {
            exchange.close();
        }

    }

    private void sendResponse(HttpExchange exchange,
                              int status, String message) throws IOException{
        byte[] body = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(status, body.length);
        try(OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}
