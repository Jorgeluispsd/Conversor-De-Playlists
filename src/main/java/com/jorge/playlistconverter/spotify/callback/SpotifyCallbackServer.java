package com.jorge.playlistconverter.spotify.callback;

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

    private void sendCallbackResponse(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(status, body.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    public CallbackSession start(String expectedState){
        CompletableFuture<String> future = new CompletableFuture<>();

        HttpServer server;

        try {
            server = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", CALLBACK_PORT), 0);
        }catch (IOException e){
            throw new CallbackServerStartupException("Não foi possível abrir o servidor callback do Spotify. "
                    + "Verifique se outra aplicação está usando o endereço configurado.",
                    e);
        }

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
                        future.completeExceptionally(new AuthorizationDeniedException(
                                "Spotify não autorizou o acesso: " + result.error()));

                        sendCallbackResponse(exchange, HTTP_BAD_REQUEST,
                                "Autorização não concluida. Volte ao terminal.");
                    }

                    case MISSING_CODE -> {
                        future.completeExceptionally(new AuthorizationCallbackException(
                                "Callback sem código de autorização"));

                        sendCallbackResponse(exchange, HTTP_BAD_REQUEST,
                                "Código de autorização ausente.");
                    }

                    case SUCCESS -> {
                        future.complete(result.code());

                        sendCallbackResponse(exchange, HTTP_OK,
                                "Autorização recebida. Volte ao terminal para acompanhar o resultado.");
                    }
                }
            }catch (IOException e) {
                log.warn("Não foi possível enviar a resposta HTTP do callback. Tipo: {}",
                        e.getClass().getSimpleName());

            }catch (RuntimeException e){
                future.completeExceptionally(new AuthorizationCallbackException(
                        "Falha ao processar o retorno de autorização do Spotify.", e));

            }finally {
                exchange.close();
            }
        });

        server.start();

        return new CallbackSession(server, future);
    }
}
