package com.jorge.playlistconverter.youtube.session;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.jorge.playlistconverter.errors.youtube.YoutubeAuthenticationException;

import java.io.IOException;

public class GoogleSessionManager {

    private static final String LOCAL_USER_ID = "local-user";
    private static final long TOKEN_EXPIRATION_MARGIN_SECONDS = 60;


    public Credential restore(AuthorizationCodeFlow flow){
        Credential storedCredential;

        try {
            storedCredential = flow.loadCredential(LOCAL_USER_ID);

        } catch (IOException e) {
            throw new YoutubeAuthenticationException(
                    "Não foi possível ler a sessão Google salva.", e);
        }

        if (storedCredential == null) {
            return null;
        }

        if (hasValidAccessToken(storedCredential)){
            return storedCredential;
        }

        return refreshCredential(flow, storedCredential);
    }

    public Credential store(AuthorizationCodeFlow flow, TokenResponse response){
        try {
            return flow.createAndStoreCredential(response, LOCAL_USER_ID);

        }catch (IOException e){
            throw new YoutubeAuthenticationException(
                    "A autorização Google foi recebida, mas não foi "
                            + "possível armazenar a sessão local.", e);
        }
    }

    private Credential refreshCredential(AuthorizationCodeFlow flow, Credential credential){
        String refreshToken = credential.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()){
            return null;
        }

        try {
            boolean refreshed = credential.refreshToken();

            if (refreshed && hasValidAccessToken(credential)) {
                return credential;
            }

            throw new YoutubeAuthenticationException(
                    "A renovação Google não produziu " + "um access token utilizável.");

        }catch (TokenResponseException e){
            if (e.getDetails() != null
                    && "invalid_grant".equals(e.getDetails().getError())){

                deleteStoredCredential(flow);
                return null;
            }

            throw new YoutubeAuthenticationException(
                    "Google recusou a renovação da sessão. "
                            + "Nenhuma nova autorização foi iniciada.");


        }catch (IOException e){
            throw new YoutubeAuthenticationException(
                    "Ocorreu uma falha técnica ao renovar a sessão Google. "
                            + "Nenhuma nova autorização foi iniciada.");
        }
    }

    private boolean hasValidAccessToken(Credential credential){
        String accessToken = credential.getAccessToken();
        Long expiresInSeconds = credential.getExpiresInSeconds();

        return accessToken != null
                && !accessToken.isBlank()
                && expiresInSeconds != null
                && expiresInSeconds > TOKEN_EXPIRATION_MARGIN_SECONDS;
    }

    private void deleteStoredCredential(AuthorizationCodeFlow flow) {
        try {
            flow.getCredentialDataStore().delete(LOCAL_USER_ID);

        } catch (IOException e) {
            throw new YoutubeAuthenticationException(
                    "Não foi possível remover a sessão Google inválida.", e);
        }
    }

}
