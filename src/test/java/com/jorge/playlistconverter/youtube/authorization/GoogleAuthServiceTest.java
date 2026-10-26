package com.jorge.playlistconverter.youtube.authorization;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.jorge.playlistconverter.errors.OperationInterruptedException;
import com.jorge.playlistconverter.errors.authorization.AuthorizationDeniedException;
import com.jorge.playlistconverter.errors.authorization.AuthorizationTimeoutException;
import com.jorge.playlistconverter.errors.youtube.YoutubeAuthenticationException;
import com.jorge.playlistconverter.youtube.callback.GoogleCallbackServer;
import com.jorge.playlistconverter.youtube.callback.GoogleCallbackSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.jorge.playlistconverter.youtube.session.GoogleSessionManager;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GoogleAuthServiceTest {

    private static final String REDIRECT_URI =
            "http://127.0.0.1:49152/callback";

    private static final String AUTHORIZATION_URL =
            "https://accounts.google.com/o/oauth2/auth?client_id=test-client";

    private AuthorizationCodeFlow flow;
    private AuthorizationCodeRequestUrl authorizationUrl;
    private AuthorizationCodeTokenRequest tokenRequest;
    private GoogleCallbackServer callbackServer;
    private GoogleCallbackSession callback;
    private GoogleAuthorizationBrowser browser;
    private TokenResponse tokenResponse;
    private Credential credential;
    private GoogleAuthService service;

    @BeforeEach
    void setUp() throws Exception {
        GoogleAuthorizationFlowFactory factory =
                mock(GoogleAuthorizationFlowFactory.class);

        flow = mock(AuthorizationCodeFlow.class);
        authorizationUrl =
                mock(AuthorizationCodeRequestUrl.class, RETURNS_SELF);

        tokenRequest =
                mock(AuthorizationCodeTokenRequest.class, RETURNS_SELF);

        callbackServer = mock(GoogleCallbackServer.class);
        callback = mock(GoogleCallbackSession.class);
        browser = mock(GoogleAuthorizationBrowser.class);
        credential = mock(Credential.class);
        GoogleSessionManager sessionManager = new GoogleSessionManager();

        tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("fake-access-token");

        when(factory.create()).thenReturn(flow);
        when(callbackServer.start(anyString())).thenReturn(callback);
        when(callback.getRedirectUri()).thenReturn(REDIRECT_URI);

        when(flow.newAuthorizationUrl()).thenReturn(authorizationUrl);
        when(authorizationUrl.build()).thenReturn(AUTHORIZATION_URL);

        when(callback.awaitCode(180, TimeUnit.SECONDS))
                .thenReturn("test-code");

        when(flow.newTokenRequest("test-code")).thenReturn(tokenRequest);
        when(tokenRequest.execute()).thenReturn(tokenResponse);

        when(flow.createAndStoreCredential(tokenResponse, "local-user"))
                .thenReturn(credential);

        service = new GoogleAuthService(factory, callbackServer, browser, sessionManager);
    }

    @AfterEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void clearInterruptedFlag() {
        Thread.interrupted();
    }

    @Test
    void shouldExchangeCodeAndStoreCredential() throws Exception {
        Credential result = service.login();

        assertSame(credential, result);

        ArgumentCaptor<String> stateCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(callbackServer).start(stateCaptor.capture());

        String state = stateCaptor.getValue();

        assertFalse(state.isBlank());
        verify(authorizationUrl).setState(state);
        verify(authorizationUrl).setRedirectUri(REDIRECT_URI);
        verify(authorizationUrl).set("prompt", "consent");

        verify(browser).open(URI.create(AUTHORIZATION_URL));
        verify(tokenRequest).setRedirectUri(REDIRECT_URI);
        verify(tokenRequest, times(1)).execute();

        verify(flow).createAndStoreCredential(
                tokenResponse, "local-user");

        verify(callback).stop();
    }

    @Test
    void shouldPreserveAuthorizationDenial() throws Exception {
        AuthorizationDeniedException original =
                new AuthorizationDeniedException("Rejeição simulada");

        when(callback.awaitCode(180, TimeUnit.SECONDS))
                .thenThrow(new ExecutionException(original));

        AuthorizationDeniedException result = assertThrows(
                AuthorizationDeniedException.class,
                () -> service.login());

        assertSame(original, result);
        verify(flow, never()).newTokenRequest(anyString());
        verify(callback).stop();
    }

    @Test
    void shouldReportAuthorizationTimeout() throws Exception {
        TimeoutException original = new TimeoutException();

        when(callback.awaitCode(180, TimeUnit.SECONDS))
                .thenThrow(original);

        AuthorizationTimeoutException result = assertThrows(
                AuthorizationTimeoutException.class,
                () -> service.login());

        assertSame(original, result.getCause());
        verify(flow, never()).newTokenRequest(anyString());
        verify(callback).stop();
    }

    @Test
    void shouldPreserveInterruption() throws Exception {
        InterruptedException original =
                new InterruptedException("Interrupção simulada");

        when(callback.awaitCode(180, TimeUnit.SECONDS))
                .thenThrow(original);

        OperationInterruptedException result = assertThrows(
                OperationInterruptedException.class,
                () -> service.login());

        assertSame(original, result.getCause());
        assertTrue(Thread.currentThread().isInterrupted());

        verify(flow, never()).newTokenRequest(anyString());
        verify(callback).stop();
    }

    @Test
    void shouldStopCallbackWhenBrowserFails() throws Exception {
        YoutubeAuthenticationException original =
                new YoutubeAuthenticationException(
                        "Falha simulada no navegador");

        doThrow(original).when(browser).open(any(URI.class));

        YoutubeAuthenticationException result = assertThrows(
                YoutubeAuthenticationException.class,
                () -> service.login());

        assertSame(original, result);

        verify(callback, never())
                .awaitCode(anyLong(), any(TimeUnit.class));

        verify(flow, never()).newTokenRequest(anyString());
        verify(callback).stop();
    }

    @Test
    void shouldHideOriginalTokenExchangeError() throws Exception {
        when(tokenRequest.execute()).thenThrow(
                new IOException("fake-sensitive-value"));

        YoutubeAuthenticationException result = assertThrows(
                YoutubeAuthenticationException.class,
                () -> service.login());

        assertTrue(result.getMessage().contains(
                "Não foi possível trocar o código"));

        assertFalse(result.getMessage().contains(
                "fake-sensitive-value"));

        assertNull(result.getCause());

        verify(flow, never()).createAndStoreCredential(
                any(TokenResponse.class), anyString());

        verify(callback).stop();
    }

    @Test
    void shouldReportCredentialStorageFailure() throws Exception {
        IOException original =
                new IOException("Falha simulada no armazenamento");

        when(flow.createAndStoreCredential(tokenResponse, "local-user"))
                .thenThrow(original);

        YoutubeAuthenticationException result = assertThrows(
                YoutubeAuthenticationException.class,
                () -> service.login());

        assertTrue(result.getMessage().contains(
                "não foi possível armazenar a sessão local"));

        assertSame(original, result.getCause());
        verify(callback).stop();
    }

    @Test
    void shouldReuseValidStoredCredential() throws Exception {
        Credential stored = mock(Credential.class);

        when(flow.loadCredential("local-user")).thenReturn(stored);
        when(stored.getAccessToken()).thenReturn("fake-access-token");
        when(stored.getExpiresInSeconds()).thenReturn(3600L);

        Credential result = service.ensureAuthenticated();

        assertSame(stored, result);

        verify(stored, never()).refreshToken();
        verifyNoInteractions(browser, callbackServer);
    }

    @Test
    void shouldLoginWhenNoStoredCredentialExists() throws Exception {
        when(flow.loadCredential("local-user")).thenReturn(null);

        Credential result = service.ensureAuthenticated();

        assertSame(credential, result);

        verify(browser).open(URI.create(AUTHORIZATION_URL));
        verify(flow).createAndStoreCredential(
                tokenResponse, "local-user");
        verify(callback).stop();
    }

    @Test
    void shouldRefreshExpiredStoredCredential() throws Exception {
        Credential stored = expiredStoredCredential();

        when(stored.getExpiresInSeconds()).thenReturn(0L, 3600L);
        when(stored.refreshToken()).thenReturn(true);

        Credential result = service.ensureAuthenticated();

        assertSame(stored, result);

        verify(stored, times(1)).refreshToken();
        verifyNoInteractions(browser, callbackServer);
    }

    @Test
    void shouldRemoveInvalidGrantAndStartNewLogin() throws Exception {
        Credential stored = expiredStoredCredential();

        TokenErrorResponse details = new TokenErrorResponse();
        details.setError("invalid_grant");

        TokenResponseException original =
                mock(TokenResponseException.class);

        when(original.getDetails()).thenReturn(details);
        when(stored.refreshToken()).thenThrow(original);

        DataStore<StoredCredential> storage =
                new MemoryDataStoreFactory()
                        .getDataStore("test_credentials");

        storage.set("local-user", new StoredCredential());

        when(flow.getCredentialDataStore()).thenReturn(storage);

        Credential result = service.ensureAuthenticated();

        assertSame(credential, result);
        assertNull(storage.get("local-user"));

        verify(browser).open(URI.create(AUTHORIZATION_URL));
        verify(flow).createAndStoreCredential(
                tokenResponse, "local-user");
        verify(callback).stop();
    }

    @Test
    void shouldNotStartLoginAfterTechnicalRefreshFailure() throws Exception {

        Credential stored = expiredStoredCredential();

        when(stored.refreshToken()).thenThrow(
                new IOException("Falha técnica simulada"));

        YoutubeAuthenticationException result = assertThrows(
                YoutubeAuthenticationException.class,
                () -> service.ensureAuthenticated());

        assertTrue(result.getMessage().contains("falha técnica"));

        verify(flow, never()).getCredentialDataStore();
        verifyNoInteractions(browser, callbackServer);
    }

    @Test
    void shouldNotStartLoginAfterStoredSessionReadFailure()
            throws Exception {

        IOException original =
                new IOException("Falha simulada ao ler a sessão");

        when(flow.loadCredential("local-user")).thenThrow(original);

        YoutubeAuthenticationException result = assertThrows(
                YoutubeAuthenticationException.class,
                () -> service.ensureAuthenticated());

        assertSame(original, result.getCause());

        verify(flow, never()).getCredentialDataStore();
        verifyNoInteractions(browser, callbackServer);
    }

    private Credential expiredStoredCredential() throws IOException {
        Credential stored = mock(Credential.class);

        when(flow.loadCredential("local-user")).thenReturn(stored);
        when(stored.getAccessToken()).thenReturn("fake-expired-token");
        when(stored.getExpiresInSeconds()).thenReturn(0L);
        when(stored.getRefreshToken()).thenReturn("fake-refresh-token");

        return stored;
    }
}
