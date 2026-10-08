package com.jorge.playlistconverter.spotify;

import com.jorge.playlistconverter.spotify.callback.CallbackStatus;
import com.jorge.playlistconverter.spotify.callback.SpotifyCallbackServer;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.BadRequestException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.sun.net.httpserver.HttpExchange;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.time.Instant;
import java.util.stream.Collectors;
import java.net.URLDecoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Getter
@Slf4j
public class SpotifyAuthService {

    private static final int CALLBACK_PORT = 8888;
    private static final String CALLBACK_PATH = "/callback";
    private static final long TOKEN_EXPIRATION_MARGIN_SECONDS = 60;
    private static final long LOGIN_TIMEOUT_SECONDS = 180;
    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;

    private final SpotifyApi spotifyApi;
    private final SpotifyCallbackServer callbackServer;
    private final SpotifyTokenStorage tokenStorage;

    private Instant accessTokenExpirationAt;
    private String activeScope;

    public SpotifyAuthService(SpotifyApi spotifyApi, SpotifyCallbackServer callbackServer){
        this.spotifyApi = spotifyApi;
        this.callbackServer = callbackServer;
        this.tokenStorage = new SpotifyTokenStorage();
    }

    public void ensureAuthenticated(String scope) throws Exception {
        if (scope ==  null || scope.isBlank())
            throw new IllegalArgumentException("O escopo não pode ser nulo ou vazio"
            );

        if (hasValidAccessToken(scope)){
            log.debug("Reutilizando access token válido");
            return;
        }

        Optional<SpotifyTokenStorage.StoredToken> stored = tokenStorage.load();

        if (stored.isPresent()) {
            if (!hasRequiredScopes(stored.get().scope(), scope)) {
                log.info("Escopo salvo ({}) difere do solicitado ({}), deletando e fazendo novo login",
                        stored.get().scope(), scope);

                tokenStorage.delete();

            } else {
                try {
                    spotifyApi.setRefreshToken(stored.get().refreshToken());

                    AuthorizationCodeCredentials refreshed =
                            spotifyApi.authorizationCodePKCERefresh().build().execute();

                    String storedScope = stored.get().scope();

                    applyCredentials(refreshed, storedScope);

                    if (refreshed.getRefreshToken() != null
                            && !refreshed.getRefreshToken().isBlank()) {
                        tokenStorage.save(refreshed.getRefreshToken(), storedScope);
                    }

                    log.info("Sessão restaurada sem novo login");
                    return;
                } catch (SpotifyWebApiException e) {
                    if (!isInvalidRefreshToken(e)){
                        log.warn("Falha na renovação Spotify; token salvo preservado. Tipo: {}",
                                e.getClass().getSimpleName()
                        );

                        throw e;

                    }

                    log.warn("Refresh token inválido; Removendo sessão salva e solicitando novo login");

                    tokenStorage.delete();


                    spotifyApi.setAccessToken(null);
                    spotifyApi.setRefreshToken(null);
                    accessTokenExpirationAt = null;
                    activeScope = null;
                }
            }
        }

        AuthorizationCodeCredentials credentials = login(scope);
        tokenStorage.save(credentials.getRefreshToken(), scope);
    }

    private boolean hasValidAccessToken(String requestedScope){
        String accessToken = spotifyApi.getAccessToken();

        return accessToken != null
                && !accessToken.isBlank()
                && accessTokenExpirationAt != null
                && Instant.now().isBefore(accessTokenExpirationAt)
                && hasRequiredScopes(activeScope, requestedScope);
    }

    public AuthorizationCodeCredentials login(String scope) throws Exception{
        log.info("Iniciando fluxo de autenticação Spotify com scope: {}", scope);

        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        String state = generateState();
        log.debug("PKCE gerado");

        URI authorizationUri = spotifyApi
                .authorizationCodePKCEUri(codeChallenge)
                .scope(scope)
                .state(state)
                .show_dialog(true)
                .build()
                .execute();

        var callback = callbackServer.start(state);

        try {
            log.info("Servidor callback iniciado na porta {}", CALLBACK_PORT);

            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {;
                Desktop.getDesktop().browse(authorizationUri);
                log.info("Navegador aberto. Aguardando autorização do usuário...");
            }else{
                log.info("Abra está URL no navegador: \n{}", authorizationUri);
            }

            String code = callback.codeFuture().get(
                    LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS
            );

            AuthorizationCodeCredentials credentials = spotifyApi
                    .authorizationCodePKCE(code, codeVerifier)
                    .build()
                    .execute();

            applyCredentials(credentials, scope);

            log.info("Autenticação concluída com sucesso! Access token expira em {} segundos",
                    credentials.getExpiresIn());

            return credentials;


        } catch (TimeoutException e) {
            throw new IllegalStateException(
                    "Tempo de autorização esgotado. Inicie uma nova tentativa.", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;

        }catch (ExecutionException e){
            throw new IllegalStateException(
                    "Falha no Callback de autorização do Spotify",
                    e.getCause());

        }finally {
            callback.server().stop(1);
            log.debug("Servidor callback encerrado");
        }
    }

    private void applyCredentials(
            AuthorizationCodeCredentials credentials,
            String scope
    ){
        spotifyApi.setAccessToken(credentials.getAccessToken());

        if (credentials.getRefreshToken() != null
                && !credentials.getRefreshToken().isBlank()){
            spotifyApi.setRefreshToken(credentials.getRefreshToken());
        }

        long usableSeconds = Math.max(
                0L,
                credentials.getExpiresIn().longValue() - TOKEN_EXPIRATION_MARGIN_SECONDS
        );

        accessTokenExpirationAt = Instant.now().plusSeconds(usableSeconds);

        activeScope = scope;
    }

    private boolean isInvalidRefreshToken(SpotifyWebApiException exception){
        if (!(exception instanceof BadRequestException)){
            return false;
        }

        String message = exception.getMessage();
        return message != null
                && message.toLowerCase(Locale.ROOT)
                .contains("invalid_grant");
    }

    private record CallbackSession(HttpServer server,
                                   CompletableFuture<String> codeFuture
    ){
    }

    private record CallbackResult(CallbackStatus status, String code, String error
    ){
    }

    private CallbackResult classifyCallback(HttpExchange exchange, String expectedState){
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();

        String receivedState = extractParam(query, "state");
        String error = extractParam(query , "error");
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

    private String generateState(){
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private void sendCallbackResponse(HttpExchange exchange, int status, String message) throws IOException{
        byte[] body = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, body.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private CallbackSession startCallbackServer(String expectedState) throws IOException{
        CompletableFuture<String> future = new CompletableFuture<>();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1",
                CALLBACK_PORT), 0);

        server.createContext(CALLBACK_PATH, exchange -> {

                    try {
                        CallbackResult result = classifyCallback(exchange, expectedState);

                        switch (result.status()) {
                            case INVALID_PATH -> sendCallbackResponse(exchange, HTTP_NOT_FOUND,
                                    "Caminho não encontrado");

                            case INVALID_METHOD -> sendCallbackResponse(exchange, HTTP_METHOD_NOT_ALLOWED,
                                    "Método não permitido");

                            case INVALID_STATE -> {
                                sendCallbackResponse(exchange, HTTP_BAD_REQUEST,
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

                    } catch (Exception e) {
                        future.completeExceptionally(e);

                    } finally {
                        exchange.close();
                    }
                });

            server.start();

            return new CallbackSession(server, future);
    }


    private String extractParam(String query, String key){
        if (query == null || query.isBlank()){
            return null;
        }

        for (String pair : query.split("&")){
            String[] parts = pair.split("=", 2);

            String name = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8
            );

            if (name.equals(key)){
                return parts.length == 2 ? URLDecoder.decode(
                        parts[1],
                        StandardCharsets.UTF_8)
                        : "";
            }
        }
        return null;
    }


    private String generateCodeVerifier(){
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));

        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private Set<String> parseScopes(String scope){
        if (scope == null || scope.isBlank()){
            return Set.of();
        }

        return Arrays.stream(scope.trim().split("\\s+"))
                .collect(Collectors.toSet());
    }

    private boolean hasRequiredScopes(
            String availableScope,
            String requestedScope
    ){
        Set<String> availableScopes = parseScopes(availableScope);
        Set<String> requestedScopes = parseScopes(requestedScope);

        return !requestedScopes.isEmpty()
                && availableScopes.containsAll(requestedScopes);
    }

    public void invalidateAccessToken(){
        spotifyApi.setAccessToken(null);
        accessTokenExpirationAt = null;
    }
}
