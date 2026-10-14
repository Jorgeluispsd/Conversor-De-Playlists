package com.jorge.playlistconverter.youtube;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTubeScopes;
import com.jorge.playlistconverter.errors.youtube.GoogleClientSecretsException;
import com.jorge.playlistconverter.errors.youtube.YoutubeAuthenticationException;
import com.jorge.playlistconverter.youtube.authorization.GoogleAuthorizationFlowFactory;
import com.jorge.playlistconverter.youtube.authorization.GoogleClientSecretsLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class GoogleAuthorizationFlowFactoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldCreateOfflineFlowWithPkceAndTemporaryStorage() throws IOException{

        GoogleClientSecretsLoader loader = loaderWithFakeSecrets();
        Path tokensDirectory = temporaryDirectory.resolve("tokens");

        AuthorizationCodeFlow flow = new GoogleAuthorizationFlowFactory(loader, tokensDirectory).create();

        assertEquals("test-client-id", flow.getClientId());
        assertTrue(flow.getScopes().contains(YouTubeScopes.YOUTUBE_FORCE_SSL));

        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl();

        assertEquals("offline", authorizationUrl.get("access_type"));
        assertEquals("S256", authorizationUrl.getCodeChallengeMethod());

        assertNotNull(authorizationUrl.getCodeChallenge());
        assertFalse(authorizationUrl.getCodeChallenge().isBlank());

        assertTrue(Files.isDirectory(tokensDirectory));

        FileDataStoreFactory storage = assertInstanceOf(FileDataStoreFactory.class,
                flow.getCredentialDataStore().getDataStoreFactory());

        assertEquals(tokensDirectory.toRealPath(),
                storage.getDataDirectory().toPath().toRealPath());

        verify(loader, times(1)).load();
    }

    @Test
    void shouldPreserveCredentialsLoadingFailure(){
        GoogleClientSecretsLoader loader = mock(GoogleClientSecretsLoader.class);

        GoogleClientSecretsException original = new GoogleClientSecretsException(
                "Falha simulada nas credenciais");

        when(loader.load()).thenThrow(original);

        Path tokensDirectory = temporaryDirectory.resolve("tokens");

        GoogleClientSecretsException result = assertThrows(
                GoogleClientSecretsException.class,
                () -> new GoogleAuthorizationFlowFactory(loader, tokensDirectory).create());

        assertSame(original, result);
        assertFalse(Files.exists(tokensDirectory));
    }

    @Test
    void shouldIdentifyStoragePreparationFailure() throws IOException{
        GoogleClientSecretsLoader loader = loaderWithFakeSecrets();

        Path invalidDirectory = temporaryDirectory.resolve("file-instead-of-directory");

        Files.writeString(invalidDirectory, "test");

        YoutubeAuthenticationException result = assertThrows(
                YoutubeAuthenticationException.class,
                () -> new GoogleAuthorizationFlowFactory(loader, invalidDirectory).create());

        assertEquals("Não foi possível preparar o armazenamento da sessão Google.",
                result.getMessage());

        assertInstanceOf(IOException.class, result.getCause());
        assertTrue(Files.isRegularFile(invalidDirectory));
    }

    private GoogleClientSecretsLoader loaderWithFakeSecrets(){
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();

        details.setClientId("test-client-id");
        details.setClientSecret("test-client-secret");
        details.setAuthUri("https://accounts.google.com/o/oauth2/auth");
        details.setTokenUri("https://oauth2.googleapis.com/token");

        GoogleClientSecrets secrets = new GoogleClientSecrets();
        secrets.setInstalled(details);

        GoogleClientSecretsLoader loader = mock(GoogleClientSecretsLoader.class);

        when(loader.load()).thenReturn(secrets);

        return loader;
    }
}
