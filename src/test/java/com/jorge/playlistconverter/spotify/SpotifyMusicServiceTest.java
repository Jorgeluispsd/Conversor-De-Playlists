package com.jorge.playlistconverter.spotify;

import com.jorge.playlistconverter.errors.OperationInterruptedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SpotifyMusicServiceTest {

    @AfterEach
    void clearInterrupetdFlag(){
        Thread.interrupted();
    }

    @Test
    void shouldPreserveInterruptionDuringInitialAuthentication() throws Exception{
        SpotifyAuthService authService = mock(SpotifyAuthService.class);
        InterruptedException original = new InterruptedException("Interrupção Simulada");

        doThrow(original).when(authService).ensureAuthenticated(anyString());

        SpotifyMusicService service = new SpotifyMusicService(authService);

        OperationInterruptedException result = assertThrows(OperationInterruptedException.class, () ->
            service.getPlaylistTracks("test-playlist")
        );

        assertSame(original, result.getCause());
        assertTrue(Thread.currentThread().isInterrupted());

        verify(authService, times(1)).ensureAuthenticated(anyString());

        verify(authService, never()).getSpotifyApi();
    }

    @Test
    void shouldPreserveInterruptionBeforeReadingPlaylistPage() throws Exception{
        SpotifyAuthService authService = mock(SpotifyAuthService.class);
        InterruptedException original = new InterruptedException("Interrupção Simulada");

        doNothing().doThrow(original).when(authService).ensureAuthenticated(anyString());

        SpotifyMusicService service = new SpotifyMusicService(authService);

        OperationInterruptedException result = assertThrows(OperationInterruptedException.class, () ->
            service.getPlaylistTracks("test-playlist")
        );

        assertSame(original, result.getCause());
        assertTrue(Thread.currentThread().isInterrupted());

        verify(authService, times(2)).ensureAuthenticated(anyString());
        verify(authService, times(1)).getSpotifyApi();
    }
}
