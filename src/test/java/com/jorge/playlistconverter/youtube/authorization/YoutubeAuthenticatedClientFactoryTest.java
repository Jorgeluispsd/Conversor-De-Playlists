package com.jorge.playlistconverter.youtube.authorization;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.youtube.YouTube;
import com.jorge.playlistconverter.errors.youtube.YoutubeAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YoutubeAuthenticatedClientFactoryTest {

    private GoogleAuthService authService;
    private YoutubeAuthenticatedClientFactory factory;

    @BeforeEach
    void setUp(){
        authService = mock(GoogleAuthService.class);
        factory = new YoutubeAuthenticatedClientFactory(authService);
    }

    @Test
    void shouldNotAuthenticateDuringConstruction(){
        verifyNoInteractions(authService);
    }

    @Test
    void shouldCreateClientWithAuthenticatedCredential(){
        Credential credential = mock(Credential.class);

        when(authService.ensureAuthenticated()).thenReturn(credential);

        YouTube client = factory.create();

        assertSame(credential, client.getRequestFactory().getInitializer());
        assertEquals("playlist-converter", client.getApplicationName());

        verify(authService, times(1)).ensureAuthenticated();
    }

    @Test
    void shouldPreserveAuthenticationFailure(){
        YoutubeAuthenticationException original =
                new YoutubeAuthenticationException("Falha de autenticação simulada.");

        when(authService.ensureAuthenticated()).thenThrow(original);

        YoutubeAuthenticationException result = assertThrows(
                YoutubeAuthenticationException.class, () -> factory.create());

        assertSame(original, result);
        verify(authService, times(1)).ensureAuthenticated();
    }
}
