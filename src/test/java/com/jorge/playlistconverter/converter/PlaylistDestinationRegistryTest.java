package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.enums.ConversionDirection;
import com.jorge.playlistconverter.errors.persistence.SyncStateStorageException;
import com.jorge.playlistconverter.model.SyncJob;
import com.jorge.playlistconverter.state.SyncStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlaylistDestinationRegistryTest {

    private SyncStateStore store;
    private PlaylistDestinationRegistry registry;

    @BeforeEach
    void setUp() {
        store = mock(SyncStateStore.class);

        registry = new PlaylistDestinationRegistry(
                store,
                ConversionDirection.SPOTIFY_TO_YOUTUBE,
                new PlaylistConversionValidator());
    }

    @Test
    void shouldFindDestinationsUsingConfiguredDirection() {
        List<SyncJob> jobs = List.of(
                new SyncJob(
                        10L,
                        "source-playlist",
                        "destination-playlist",
                        "SPOTIFY_TO_YOUTUBE"));

        when(store.findSyncJobsBySource(
                "source-playlist",
                "SPOTIFY_TO_YOUTUBE")).thenReturn(jobs);

        List<SyncJob> result =
                registry.findDestinations("source-playlist");

        assertEquals(jobs, result);

        verify(store).findSyncJobsBySource(
                "source-playlist",
                "SPOTIFY_TO_YOUTUBE");

        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldRegisterDestinationThroughGetOrCreateJob() {
        when(store.getOrCreateSyncJob(
                "source-playlist",
                "destination-playlist",
                "SPOTIFY_TO_YOUTUBE")).thenReturn(10L);

        long result = registry.registerDestination(
                "source-playlist",
                "destination-playlist");

        assertEquals(10L, result);

        verify(store).getOrCreateSyncJob(
                "source-playlist",
                "destination-playlist",
                "SPOTIFY_TO_YOUTUBE");

        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldRejectInvalidIdsBeforeAccessingStorage() {
        for (String invalid : new String[]{null, "", "   "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> registry.findDestinations(invalid));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> registry.registerDestination(
                            invalid, "destination-playlist"));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> registry.registerDestination(
                            "source-playlist", invalid));
        }

        verifyNoInteractions(store);
    }

    @Test
    void shouldPreserveReadFailureInsteadOfReturningEmptyDestinations() {
        SyncStateStorageException original =
                new SyncStateStorageException(
                        "Falha simulada.",
                        new SQLException("Falha simulada"));

        when(store.findSyncJobsBySource(
                "source-playlist",
                "SPOTIFY_TO_YOUTUBE")).thenThrow(original);

        SyncStateStorageException result = assertThrows(
                SyncStateStorageException.class,
                () -> registry.findDestinations("source-playlist"));

        assertSame(original, result);

        verify(store).findSyncJobsBySource(
                "source-playlist",
                "SPOTIFY_TO_YOUTUBE");

        verifyNoMoreInteractions(store);
    }
}
