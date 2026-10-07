package com.jorge.playlistconverter.spotify.callback;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SpotifyCallbackServer {

    private static final int CALLBACK_PORT = 8888;
    private static final String CALLBACK_PATH = "/callback";

    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;

    private CallbackResult classifyCallback(HttpExchange exchange,String expectedState){

        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();

        String receivedState = extractParam(query, "state");
        String error = extractParam(query, "error");
        String code = extractParam(query, "code");

        CallbackStatus status = switch (path){
            case String p when !CALLBACK_PATH.equals(p) ->
                CallbackStatus.INVALID_PATH;

            case String p when !"GET".equals(exchange.getRequestMethod()) ->
                CallbackStatus.INVALID_METHOD;

            case String p when !expectedState.equals(receivedState) ->
                CallbackStatus.INVALID_STATE;

            case String p when error != null ->
                CallbackStatus.AUTHORIZATION_DENIED;

            case String p when code == null || code.isBlank() ->
                CallbackStatus.MISSING_CODE;

            default -> CallbackStatus.SUCCESS;
        };

        return new CallbackResult(status, code, error);
    }

    private String extractParam(String query, String key){

        if (query == null || query.isBlank()){
            return null;
        }

        for (String pair: query.split("&")){
            String[] parts = pair.split("=", 2);

            String name = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8);

            if (name.equals(key)){
                return parts.length == 2 ?
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                        : "";
            }
        }

        return null;
    }

    private void sendCallbackResponse(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(status, body.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    public CallbackSession start(String expectedState) throws IOException{
        CompletableFuture<String> future = new CompletableFuture<>();

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", CALLBACK_PORT), 0);

        server.createContext(CALLBACK_PATH, exchange -> {
            try {

                CallbackResult result = classifyCallback(exchange, expectedState);

                switch (result.status()){
                    case INVALID_PATH -> sendCallbackResponse(
                            exchange, HTTP_NOT_FOUND, "Caminho não encontrado");

                    case INVALID_METHOD -> sendCallbackResponse(
                            exchange, HTTP_METHOD_NOT_ALLOWED, "Método não permitido");

                    case INVALID_STATE -> {
                        sendCallbackResponse(
                            exchange, HTTP_BAD_REQUEST,
                            "Retorno inválido: state não corresponde ao login iniciado.");

                        log.warn("Callback recebido com state inválido");
                    }

                    case AUTHORIZATION_DENIED -> {
                        sendCallbackResponse(exchange, HTTP_BAD_REQUEST,
                                "Autorização não concluida. Volte ao terminal.");

                        future.completeExceptionally(new IllegalStateException(
                                "Spotify não autorizou o acesso: " + result.error()));
                    }

                    case MISSING_CODE -> {
                        sendCallbackResponse(exchange, HTTP_BAD_REQUEST,
                                "Código de autorização ausente.");

                        future.completeExceptionally(new IllegalStateException(
                                "Callback sem código de autorização"));
                    }

                    case SUCCESS -> {
                        sendCallbackResponse(exchange, HTTP_OK,
                                "Autorização recebida. Volte ao terminal para acompanhar o resultado.");

                        future.complete(result.code());
                    }
                }
            }catch (Exception e){
                future.completeExceptionally(e);

            }finally {
                exchange.close();
            }
        });

        server.start();

        return new CallbackSession(server, future);
    }
}
