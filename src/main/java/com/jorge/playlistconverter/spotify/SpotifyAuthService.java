package com.jorge.playlistconverter.spotify;

import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter
@Slf4j
public class SpotifyAuthService {

    private static final int CALLBACK_PORT = 8888;
    private static final String CALLBACK_PATH = "/callback";

    private final SpotifyApi spotifyApi;
    private final SpotifyTokenStorage tokenStorage;

    public SpotifyAuthService(SpotifyApi spotifyApi){
        this.spotifyApi = spotifyApi;
        this.tokenStorage = new SpotifyTokenStorage();
    }

    public void ensureAuthenticated(String scope) throws Exception {
        Optional<SpotifyTokenStorage.StoredToken> stored = tokenStorage.load();

        if (stored.isPresent()) {
            if (!stored.get().scope().equals(scope)) {
                log.info("Escopo salvo ({}) difere do solicitado ({}), deletando e fazendo novo login",
                        stored.get().scope(), scope);
                tokenStorage.delete();

            } else {
                try {
                    spotifyApi.setRefreshToken(stored.get().refreshToken());

                    AuthorizationCodeCredentials refreshed =
                            spotifyApi.authorizationCodePKCERefresh().build().execute();

                    spotifyApi.setAccessToken(refreshed.getAccessToken());

                    if (refreshed.getRefreshToken() != null) {
                        spotifyApi.setRefreshToken(refreshed.getRefreshToken());
                        tokenStorage.save(refreshed.getRefreshToken(), scope);
                    }

                    log.info("Sessão restaurada sem novo login");
                    return;
                } catch (SpotifyWebApiException e) {
                    log.warn("Refresh token recusado pelo Spotify; Novo login necessário");
                    tokenStorage.delete();
                }
            }
        }

        AuthorizationCodeCredentials credentials = login(scope);
        tokenStorage.save(credentials.getRefreshToken(), scope);
    }

    public AuthorizationCodeCredentials login(String scope) throws Exception{
        log.info("Iniciando fluxo de autenticação Spotify com scope: {}", scope);

        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        log.debug("PKCE gerado");

        URI authorizationUri = spotifyApi.authorizationCodePKCEUri(codeChallenge)
                .scope(scope)
                .show_dialog(true)
                .build()
                .execute();
        log.info("URL de autorização: {}", authorizationUri);

        CompletableFuture<String> codeFuture = startCallbackServer();
        log.info("Servidor callback iniciado na porta {}", CALLBACK_PORT);

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {;
            Desktop.getDesktop().browse(authorizationUri);
            log.info("Navegador aberto. Aguardando autorização do usuário...");
        }else{
            log.warn("Desktop não suportado (modo headless). Por favor, abra manualmente: \n{}", authorizationUri);
        }

        String code = codeFuture.get();
        log.info("Código de autorização recebido");

        AuthorizationCodeCredentials credentials = spotifyApi.
                authorizationCodePKCE(code, codeVerifier)
                .build()
                .execute();

        spotifyApi.setAccessToken(credentials.getAccessToken());
        spotifyApi.setRefreshToken(credentials.getRefreshToken());

        log.info("Autenticação concluída com sucesso! Access token expira em {} segundos", credentials.getExpiresIn());
        return credentials;
    }

    private CompletableFuture<String> startCallbackServer() throws IOException{
        CompletableFuture<String> future = new CompletableFuture<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(CALLBACK_PORT), 0);

        server.createContext(CALLBACK_PATH, exchange -> {
            String code = extractParam(exchange.getRequestURI().getQuery(), "code");

            String response = code != null
                    ? "Login concluído! Pode fechar esta aba e voltar ao terminal."
                    : "Falha na autorização. Pode fechar esta aba.";

            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try(OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            if (code != null){
                log.debug("Código capturado no callback");
                future.complete(code);
            }else {
                log.error("Autorização negada ou código ausente no callback");
                future.completeExceptionally(new RuntimeException("Autorização negada ou código ausente"));
            }

            server.stop(1);
            log.debug("Servidor callback parado");
        });

        server.start();
        return future;
    }


    private String extractParam(String query, String key){
        if (query == null) return null;

        for (String pair : query.split("&")){
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)){
                return parts[1];
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
}
