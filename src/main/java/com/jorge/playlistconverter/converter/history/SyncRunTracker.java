package com.jorge.playlistconverter.converter.history;

import com.jorge.playlistconverter.converter.ConversionResult;
import com.jorge.playlistconverter.enums.SyncRunStatus;
import com.jorge.playlistconverter.state.SyncStateStore;

import java.util.Objects;
import java.util.function.Supplier;

public class SyncRunTracker {

    private final SyncStateStore store;

    public SyncRunTracker(SyncStateStore store) {
        this.store = Objects.requireNonNull(store);
    }

    public ConversionResult execute(long syncJobId, Supplier<ConversionResult> operation) {
        Objects.requireNonNull(operation, "A operação da conversão deve ser informada.");

        long syncRunId = store.startSyncRun(syncJobId);

        ConversionResult result;

        try {
            result = operation.get();

        }catch (RuntimeException original){
            recordFailure(syncRunId, original);
            throw original;
        }

        store.finishSyncRun(syncRunId, SyncRunStatus.SUCCESS);

        return result;
    }

    private void recordFailure(long syncRunId, RuntimeException original){

        try {
            store.finishSyncRun(syncRunId, SyncRunStatus.FAILED);

        }catch (RuntimeException historyFailure){
            original.addSuppressed(historyFailure);
        }
    }
}
