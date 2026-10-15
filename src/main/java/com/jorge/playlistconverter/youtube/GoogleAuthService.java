package com.jorge.playlistconverter.youtube;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.jorge.playlistconverter.errors.OperationInterruptedException;
import com.jorge.playlistconverter.errors.authorization.AuthorizationCallbackException;
import com.jorge.playlistconverter.errors.authorization.AuthorizationDeniedException;
import com.jorge.playlistconverter.errors.authorization.AuthorizationTimeoutException;
import com.jorge.playlistconverter.errors.youtube.YoutubeAuthenticationException;
import com.jorge.playlistconverter.youtube.authorization.GoogleAuthorizationBrowser;
import com.jorge.playlistconverter.youtube.authorization.GoogleAuthorizationFlowFactory;
import com.jorge.playlistconverter.youtube.callback.GoogleCallbackServer;
import com.jorge.playlistconverter.youtube.callback.GoogleCallbackSession;
import com.jorge.playlistconverter.youtube.session.GoogleSessionManager;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GoogleAuthService {

    private static final long LOGIN_TIMEOUT_SECONDS = 180;

    private final GoogleAuthorizationFlowFactory flowFactory;
    private final GoogleCallbackServer callbackServer;
    private final GoogleAuthorizationBrowser browser;
    private final SecureRandom secureRandom = new SecureRandom();
    private final GoogleSessionManager sessionManager;

    public GoogleAuthService(GoogleAuthorizationFlowFactory flowFactory, GoogleCallbackServer callbackServer,
                             GoogleAuthorizationBrowser browser, GoogleSessionManager sessionManager) {
        this.flowFactory = Objects.requireNonNull(flowFactory);
        this.callbackServer = Objects.requireNonNull(callbackServer);
        this.browser = Objects.requireNonNull(browser);
        this.sessionManager = Objects.requireNonNull(sessionManager);
    }

    public Credential login(){
        return login(flowFactory.create());
    }

    public Credential ensureAuthenticated(){
        AuthorizationCodeFlow flow = flowFactory.create();

        Credential storedCredential = sessionManager.restore(flow);

        return storedCredential != null
                ? storedCredential : login(flow);
    }

    private Credential login(AuthorizationCodeFlow flow){
        String state = generateState();

        GoogleCallbackSession callback = callbackServer.start(state);

        try {
            String authorizationUrl = flow.newAuthorizationUrl()
                    .setRedirectUri(callback.getRedirectUri())
                    .setState(state)
                    .set("prompt", "consent")
                    .build();

            browser.open(URI.create(authorizationUrl));

            String code = callback.awaitCode(LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            TokenResponse response = exchangeCode(flow, code, callback.getRedirectUri());

            return sessionManager.store(flow, response);

        }catch (TimeoutException e){
            throw new AuthorizationTimeoutException("Tempo de autorização Google esgotado. "
                    + "Feche a aba desta tentativa e inicie uma nova autorização.", e);

        }catch (InterruptedException e){
            Thread.currentThread().interrupt();

            throw new OperationInterruptedException(
                    "Autorização Google interrompida.", e);

        }catch (ExecutionException e){
            Throwable cause = e.getCause();

            if (cause instanceof AuthorizationDeniedException denied){
                throw denied;
            }

            if (cause instanceof AuthorizationCallbackException failure){
                throw failure;
            }

            throw new AuthorizationCallbackException(
                    "Falha ao receber o retorno de autorização Google.", cause);

        }finally {
            callback.stop();
        }
    }

    private String generateState(){
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(bytes);
    }

    private TokenResponse exchangeCode(AuthorizationCodeFlow flow, String code, String redirectUri){
        TokenResponse response;

        try {
            response = flow.newTokenRequest(code)
                    .setRedirectUri(redirectUri).execute();

        }catch (IOException e){
            throw new YoutubeAuthenticationException(
                    "Não foi possível trocar o código de autorização "
                            + "por tokens Google. Inicie uma nova tentativa.");
        }

        if (response == null
            || response.getAccessToken() == null
            || response.getAccessToken().isBlank()){

            throw new YoutubeAuthenticationException(
                    "A resposta Google não contém um access token válido.");
        }

        return response;
    }
}
