package com.jorge.playlistconverter.spotify;

import com.jorge.playlistconverter.spotify.authorization.SpotifyAuthorizationGenerator;
import com.jorge.playlistconverter.spotify.callback.CallbackSession;
import com.jorge.playlistconverter.spotify.callback.SpotifyCallbackServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.BadRequestException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.awt.*;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class SpotifyAuthService {

    private static final long TOKEN_EXPIRATION_MARGIN_SECONDS = 60;
    private static final long LOGIN_TIMEOUT_SECONDS = 180;


    private final SpotifyApi spotifyApi;
    private final SpotifyCallbackServer callbackServer;
    private final SpotifyTokenStorage tokenStorage;
    private final SpotifyAuthorizationGenerator authorizationGenerator;

    private Instant accessTokenExpirationAt;
    private String activeScope;

    public SpotifyAuthService(SpotifyApi spotifyApi, SpotifyCallbackServer callbackServer,
                              SpotifyAuthorizationGenerator authorizationGenerator){
        this.spotifyApi = spotifyApi;
        this.callbackServer = callbackServer;
        this.authorizationGenerator = authorizationGenerator;
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

        String codeVerifier = authorizationGenerator.generateCodeVerifier();
        String codeChallenge = authorizationGenerator.generateCodeChallenge(codeVerifier);
        String state = authorizationGenerator.generateState();
        log.debug("PKCE gerado");

        URI authorizationUri = spotifyApi
                .authorizationCodePKCEUri(codeChallenge)
                .scope(scope)
                .state(state)
                .show_dialog(true)
                .build()
                .execute();

        CallbackSession callback = callbackServer.start(state);

        try {
            log.info("Servidor callback iniciado. Aguardando autorização.");

            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {;
                Desktop.getDesktop().browse(authorizationUri);
                log.info("Navegador aberto. Aguardando autorização do usuário...");
            }else{
                log.info("Abra está URL no navegador: \n{}", authorizationUri);
            }

            String code = callback.awaitCode(
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
            callback.stop();
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
