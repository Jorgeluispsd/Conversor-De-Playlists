package com.jorge.playlistconverter.state.database;

import com.jorge.playlistconverter.errors.persistence.SyncStateStorageException;
import com.jorge.playlistconverter.model.SyncJob;
import com.jorge.playlistconverter.state.sql.SqlResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class H2SyncStateStoreTest {

    private static final String SOURCE = "test-source";
    private static final String DESTINATION = "test-destination";
    private static final String DIRECTION = "SPOTIFY_TO_YOUTUBE";

    @TempDir
    Path temporaryDirectory;

    private H2SyncStateStore store;

    @BeforeEach
    void setUp() throws SQLException {
        SqlResourceLoader loader = new SqlResourceLoader();

        String dbPath = temporaryDirectory
                .resolve("test-data").toString();

        String schema = loader.read("sql/schema.sql");

        try (Connection connection =
                     DriverManager.getConnection("jdbc:h2:" + dbPath);
             Statement statement = connection.createStatement()) {

            for (String command : schema.split(";")) {
                if (!command.trim().isEmpty()) {
                    statement.execute(command.trim());
                }
            }
        }

        H2DatabaseInitializer initializer = mock(H2DatabaseInitializer.class);

        when(initializer.initialize()).thenReturn(dbPath);

        store = new H2SyncStateStore(loader, initializer);
    }

    @Test
    void shouldPreserveSqlCauseForDuplicateJob() {
        long existingId = store.createSyncJob(SOURCE, DESTINATION, DIRECTION);

        SyncStateStorageException result = assertThrows(
                SyncStateStorageException.class,
                () -> store.createSyncJob(SOURCE, DESTINATION, DIRECTION));

        SQLException cause = assertInstanceOf(
                SQLException.class, result.getCause());

        assertEquals("23505", cause.getSQLState());

        OptionalLong found = store.findSyncJob(SOURCE, DESTINATION, DIRECTION);

        assertTrue(found.isPresent());
        assertEquals(existingId, found.getAsLong());
    }

    @Test
    void shouldRecoverExistingJobAfterDuplicateInsert() {
        long existingId = store.createSyncJob(SOURCE, DESTINATION, DIRECTION);

        H2SyncStateStore observedStore = spy(store);

        doReturn(OptionalLong.empty()).doCallRealMethod()
                .when(observedStore).findSyncJob(SOURCE, DESTINATION, DIRECTION);

        long recoveredId = observedStore.getOrCreateSyncJob(SOURCE, DESTINATION, DIRECTION);

        assertEquals(existingId, recoveredId);

        verify(observedStore, times(2))
                .findSyncJob(SOURCE, DESTINATION, DIRECTION);

        verify(observedStore, times(1))
                .createSyncJob(SOURCE, DESTINATION, DIRECTION);
    }

    @Test
    void shouldPropagateNonDuplicateSqlFailure() {
        H2SyncStateStore observedStore = spy(store);

        SyncStateStorageException result = assertThrows(
                SyncStateStorageException.class,
                () -> observedStore.getOrCreateSyncJob(
                        null, DESTINATION, DIRECTION));

        SQLException cause = assertInstanceOf(SQLException.class, result.getCause());

        assertEquals("23502", cause.getSQLState());

        verify(observedStore, times(1))
                .findSyncJob(null, DESTINATION, DIRECTION);

        verify(observedStore, times(1))
                .createSyncJob(null, DESTINATION, DIRECTION);
    }

    @Test
    void shouldReturnEmptyListWhenSourceHasNoSavedDestination() {
        List<SyncJob> result =
                store.findSyncJobsBySource(SOURCE, DIRECTION);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldListOnlyDestinationsForRequestedSourceAndDirection() {
        long firstId =
                store.createSyncJob(SOURCE, "destination-A", DIRECTION);

        long secondId =
                store.createSyncJob(SOURCE, "destination-B", DIRECTION);

        store.createSyncJob(
                "another-source",
                "destination-C",
                DIRECTION
        );

        store.createSyncJob(
                SOURCE,
                "destination-D",
                "YOUTUBE_TO_SPOTIFY"
        );

        List<SyncJob> result =
                store.findSyncJobsBySource(SOURCE, DIRECTION);

        assertEquals(
                List.of(
                        new SyncJob(
                                firstId, SOURCE, "destination-A", DIRECTION),
                        new SyncJob(
                                secondId, SOURCE, "destination-B", DIRECTION)
                ),
                result
        );
    }
}
