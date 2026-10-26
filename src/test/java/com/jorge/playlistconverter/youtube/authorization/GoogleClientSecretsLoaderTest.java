package com.jorge.playlistconverter.youtube.authorization;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.jorge.playlistconverter.errors.youtube.GoogleClientSecretsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GoogleClientSecretsLoaderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldLoadValidDesktopCredentials() throws IOException{
        Path file = writeCredentials("""
                {
                  "installed": {
                    "client_id": "test-client-id",
                    "client_secret": "test-client-secret",
                    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                    "token_uri": "https://oauth2.googleapis.com/token"
                  }
                }
                """);

        GoogleClientSecrets result = new GoogleClientSecretsLoader(file).load();

        assertNotNull(result.getInstalled());
        assertNull(result.getWeb());
        assertEquals("test-client-id", result.getInstalled().getClientId());
        assertEquals("test-client-secret", result.getInstalled().getClientSecret());
    }

    @Test
    void shouldIdentifyMissingCredentialsFile(){
        Path missingFile = temporaryDirectory.resolve("missing-credentials.json");

        GoogleClientSecretsException result = assertThrows(
                GoogleClientSecretsException.class,
                () -> new GoogleClientSecretsLoader(missingFile).load());

        assertTrue(result.getMessage().contains("não encontrado"));
        assertInstanceOf(NoSuchFileException.class, result.getCause());
    }

    @Test
    void shouldRejectInvalidJsonWithoutExposingItsContent() throws IOException{
        Path file = writeCredentials("""
                {
                  "installed": {
                     "client_secret": "fake-sensitive-value",
                     "client_id":
                """);

        GoogleClientSecretsException result = assertThrows(
                GoogleClientSecretsException.class,
                () -> new GoogleClientSecretsLoader(file).load());

        assertEquals("O arquivo de credenciais Google contém um JSON inválido.",
                result.getMessage());

        assertFalse(result.getMessage().contains("fake-sensitive-value"));
        assertNull(result.getCause());
    }

    @Test
    void shouldRejectWebClientCredentials() throws IOException{
        Path file = writeCredentials("""
                {
                  "web": {
                     "client_id": "test-client-id",
                     "client_secret": "test-client-secret",
                     "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                     "token_uri": "https://oauth2.googleapis.com/token"
                  }
                }
                """);

        GoogleClientSecretsException result = assertThrows(
                GoogleClientSecretsException.class,
                () -> new GoogleClientSecretsLoader(file).load());

        assertEquals("Use credenciais OAuth do tipo Aplicativo para computador.",
                result.getMessage());
    }

    @Test
    void shouldRejectIncompleteDesktopCredentials() throws IOException{
        Path file = writeCredentials("""
                {
                  "installed": {
                  "client_id": "test-client-id",
                  "client_secret": "   ",
                  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                  "token_uri": "https://oauth2.googleapis.com/token"
                  }
                }
                """);

        GoogleClientSecretsException result = assertThrows(
                GoogleClientSecretsException.class,
                () -> new GoogleClientSecretsLoader(file).load());

        assertTrue(result.getMessage().contains("incompletas"));
        assertNull(result.getCause());
    }

    @Test
    void shouldPreserveFileReadFailure(){
        GoogleClientSecretsException result = assertThrows(
                GoogleClientSecretsException.class,
                () -> new GoogleClientSecretsLoader(temporaryDirectory).load());

        assertEquals("Não foi possível ler o arquivo de credenciais Google.",
                result.getMessage());

        assertInstanceOf(IOException.class, result.getCause());
    }

    private Path writeCredentials(String content) throws IOException{
        Path file = temporaryDirectory.resolve("test-credentials.json");

        Files.writeString(file, content, StandardCharsets.UTF_8);

        return file;
    }
}
