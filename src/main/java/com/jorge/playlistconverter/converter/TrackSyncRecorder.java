package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.state.SyncStateStore;

import java.util.Objects;
import java.util.function.Consumer;

public class TrackSyncRecorder {

    private static final String SUCCESS_STATUS = "success";

    private final SyncStateStore store;

    public TrackSyncRecorder(SyncStateStore store) {
        this.store = Objects.requireNonNull(store);
    }

    public void recordInsertedTrack(long syncJobId, MatchResult match){
        Objects.requireNonNull(match, "A correspondência da música deve ser informada.");

        if (!match.found()){
            throw new IllegalArgumentException("Não é possível registrar uma inserção "
                            + "sem correspondência no destino.");
        }

        store.markSynced(syncJobId, match.sourceSong(), match.matchedId(),
                SUCCESS_STATUS, match.confidence());
    }

    public Consumer<String> insertedListener(long syncJobId, PlaylistPreview preview){
        Objects.requireNonNull(preview, "A prévia deve ser informada.");
        return insertedId -> preview.matches().stream()
                .filter(MatchResult::found)
                .filter(match -> match.matchedId().equals(insertedId))
                .forEach(match -> recordInsertedTrack(syncJobId, match));
    }
}
