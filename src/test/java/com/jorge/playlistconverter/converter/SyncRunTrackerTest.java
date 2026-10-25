package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.enums.SyncRunStatus;
import com.jorge.playlistconverter.errors.persistence.SyncStateStorageException;
import com.jorge.playlistconverter.state.SyncStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SyncRunTrackerTest {

    private SyncStateStore store;
    private Runnable operation;
    private SyncRunTracker tracker;

    private final ConversionResult expectedResult =
            new ConversionResult("source-playlist",
                    "destination-playlist", List.of(),
                    0, 0);

    @BeforeEach
    void setUp() {
        store = mock(SyncStateStore.class);
        operation = mock(Runnable.class);

        tracker = new SyncRunTracker(store);

        when(store.startSyncRun(10L)).thenReturn(20L);
    }

    @Test
    void shouldStartRunBeforeOperationAndFinishWithSuccess() {
        ConversionResult result =
                tracker.execute(10L, this::performConversion);

        assertSame(expectedResult, result);

        var order = inOrder(store, operation);

        order.verify(store).startSyncRun(10L);
        order.verify(operation).run();
        order.verify(store).finishSyncRun(
                20L, SyncRunStatus.SUCCESS);

        verifyNoMoreInteractions(store, operation);
    }

    @Test
    void shouldRecordFailureAndPreserveOriginalException() {
        IllegalStateException original =
                new IllegalStateException("Falha simulada na operação.");

        doThrow(original).when(operation).run();

        IllegalStateException result = assertThrows(
                IllegalStateException.class,
                () -> tracker.execute(10L, this::performConversion));

        assertSame(original, result);

        var order = inOrder(store, operation);

        order.verify(store).startSyncRun(10L);
        order.verify(operation).run();
        order.verify(store).finishSyncRun(
                20L, SyncRunStatus.FAILED);

        verifyNoMoreInteractions(store, operation);
    }

    @Test
    void shouldAttachHistoryFailureWithoutReplacingOriginalException() {
        IllegalStateException original =
                new IllegalStateException("Falha simulada na operação.");

        SyncStateStorageException historyFailure =
                new SyncStateStorageException("Falha ao registrar o resultado.",
                        new SQLException("Falha simulada"));

        doThrow(original).when(operation).run();

        doThrow(historyFailure).when(store).finishSyncRun(
                20L, SyncRunStatus.FAILED);

        IllegalStateException result = assertThrows(
                IllegalStateException.class,
                () -> tracker.execute(10L, this::performConversion));

        assertSame(original, result);
        assertEquals(1, result.getSuppressed().length);
        assertSame(historyFailure, result.getSuppressed()[0]);

        verify(operation, times(1)).run();
        verify(store, times(1)).finishSyncRun(
                20L, SyncRunStatus.FAILED);
    }

    @Test
    void shouldNotMarkOperationAsFailedWhenSuccessRecordingFails() {
        SyncStateStorageException historyFailure =
                new SyncStateStorageException("Falha ao registrar sucesso.",
                        new SQLException("Falha simulada"));

        doThrow(historyFailure).when(store).finishSyncRun(
                20L, SyncRunStatus.SUCCESS);

        SyncStateStorageException result = assertThrows(
                SyncStateStorageException.class,
                () -> tracker.execute(10L, this::performConversion));

        assertSame(historyFailure, result);

        verify(operation, times(1)).run();
        verify(store, times(1)).finishSyncRun(
                20L, SyncRunStatus.SUCCESS);

        verify(store, never()).finishSyncRun(
                20L, SyncRunStatus.FAILED);
    }

    @Test
    void shouldNotExecuteOperationWhenStartingRunFails() {
        SyncStateStorageException original =
                new SyncStateStorageException(
                        "Falha ao iniciar execução.",
                        new SQLException("Falha simulada"));

        when(store.startSyncRun(10L)).thenThrow(original);

        SyncStateStorageException result = assertThrows(
                SyncStateStorageException.class,
                () -> tracker.execute(10L, this::performConversion));

        assertSame(original, result);

        verifyNoInteractions(operation);
        verify(store).startSyncRun(10L);
        verifyNoMoreInteractions(store);
    }

    private ConversionResult performConversion() {
        operation.run();
        return expectedResult;
    }
}
