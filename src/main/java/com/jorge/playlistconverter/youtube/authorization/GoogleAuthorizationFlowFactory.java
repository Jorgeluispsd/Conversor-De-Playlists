package com.jorge.playlistconverter.youtube.authorization;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTubeScopes;
import com.jorge.playlistconverter.errors.youtube.YoutubeAuthenticationException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class GoogleAuthorizationFlowFactory {

    private final GoogleClientSecretsLoader secretsLoader;
    private final Path tokensDirectory;

    public GoogleAuthorizationFlowFactory(GoogleClientSecretsLoader secretsLoader){
        this(secretsLoader, Path.of(
                System.getProperty("user.home"),
                ".playlist-converter",
                "google-tokens"));
    }

    public GoogleAuthorizationFlowFactory(GoogleClientSecretsLoader secretsLoader,
                                          Path tokensDirectory) {
        this.secretsLoader = Objects.requireNonNull(secretsLoader);
        this.tokensDirectory = Objects.requireNonNull(tokensDirectory);
    }

    public AuthorizationCodeFlow create(){
        GoogleClientSecrets secrets = secretsLoader.load();
        GoogleClientSecrets.Details details = secrets.getInstalled();

        try {
            FileDataStoreFactory dataStore = new FileDataStoreFactory(tokensDirectory.toFile());

            AuthorizationCodeFlow.Builder builder = new AuthorizationCodeFlow.Builder(
                    BearerToken.authorizationHeaderAccessMethod(),
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new GenericUrl(details.getTokenUri()),
                    new ClientParametersAuthentication(
                            details.getClientId(),
                            details.getClientSecret()
                    ),
                    details.getClientId(),
                    details.getAuthUri());

            builder.setScopes(List.of(YouTubeScopes.YOUTUBE_FORCE_SSL));
            builder.setDataStoreFactory(dataStore);
            builder.enablePKCE();

            return new GooglePkceAuthorizationFlow(builder);

        } catch (IOException e) {
            throw new YoutubeAuthenticationException("Não foi possível preparar o armazenamento "
                    + "da sessão Google.", e);
        }
    }
}
