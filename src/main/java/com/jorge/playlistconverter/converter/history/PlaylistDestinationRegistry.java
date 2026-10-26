package com.jorge.playlistconverter.converter.history;

import com.jorge.playlistconverter.converter.PlaylistConversionValidator;
import com.jorge.playlistconverter.enums.ConversionDirection;
import com.jorge.playlistconverter.model.SyncJob;
import com.jorge.playlistconverter.state.SyncStateStore;

import java.util.List;
import java.util.Objects;

public class PlaylistDestinationRegistry {

    private final SyncStateStore store;
    private final ConversionDirection direction;
    private final PlaylistConversionValidator validator;

    public PlaylistDestinationRegistry(SyncStateStore store, ConversionDirection direction,
                                       PlaylistConversionValidator validator) {
        this.store = Objects.requireNonNull(store);
        this.direction = Objects.requireNonNull(direction);
        this.validator = Objects.requireNonNull(validator);
    }

    public List<SyncJob> findDestinations(String sourcePlaylistId){
        if (sourcePlaylistId == null || sourcePlaylistId.isBlank()){
            throw new IllegalArgumentException("O ID da playlist de origem deve ser informado.");
        }

        return store.findSyncJobsBySource(sourcePlaylistId, direction.name());
    }

    public long registerDestination(String sourcePlaylistId, String destinationPlaylistId){
        validator.validatePlaylistIds(sourcePlaylistId, destinationPlaylistId);

        return store.getOrCreateSyncJob(sourcePlaylistId, destinationPlaylistId, direction.name());
    }
}
