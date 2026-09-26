package com.jorge.playlistconverter.state;

import java.util.List;

/**
 * Fase 5: implementar com SqliteSyncStateStore (JDBC + sqlite-jdbc).
 * Schema sugerido (sync_job, sync_run, synced_track) já discutido no planejamento.
 * Não implementar antes da Fase 4 estar validada.
 */
public interface SyncStateStore {

    boolean isAlreadySynced(long syncJobId, String sourceTrackId);

    void markSynced(long syncJobId, String sourceTrackId, String destinationId, double confidence);

    List<String> getHistory(long syncJobId);
}
