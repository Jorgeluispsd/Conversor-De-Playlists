package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.enums.PlaylistVisibility;
import com.jorge.playlistconverter.errors.persistence.SyncStateStorageException;
import com.jorge.playlistconverter.service.MusicService;

import java.util.List;
import java.util.Objects;

public class NewPlaylistExecutor {

    private final MusicService destination;
    private final PlaylistConversionValidator validator;
    private final PlaylistDestinationRegistry destinationRegistry;
    private final SyncRunTracker runTracker;
    private final TrackSyncRecorder trackSyncRecorder;

    public NewPlaylistExecutor(MusicService destination, PlaylistConversionValidator validator,
                               PlaylistDestinationRegistry destinationRegistry,
                               SyncRunTracker runTracker, TrackSyncRecorder trackSyncRecorder) {
        this.destination = Objects.requireNonNull(destination);
        this.validator = Objects.requireNonNull(validator);
        this.destinationRegistry = Objects.requireNonNull(destinationRegistry);
        this.runTracker = Objects.requireNonNull(runTracker);
        this.trackSyncRecorder = Objects.requireNonNull(trackSyncRecorder);
    }

    public ConversionResult execute(PlaylistPreview preview, String destinationName,
                                    PlaylistVisibility visibility){
        validator.validateForNewPlaylist(preview, destinationName, visibility);

        List<String> idsToInsert = preview.newTrackIds();

        String destinationPlaylistId = destination.createPlaylist(destinationName.strip(),
                visibility);

        long syncJobId = registerCreatedDestination(preview.sourcePlaylistId(), destinationPlaylistId);

        return runTracker.execute(syncJobId, () -> insertTracks(preview, destinationPlaylistId, idsToInsert, syncJobId));
    }

    private ConversionResult insertTracks(PlaylistPreview preview, String destinationPlaylistId,
                                          List<String> idsToInsert, long syncJobId){
        destination.addTracks(destinationPlaylistId, idsToInsert,
                trackSyncRecorder.insertedListener(syncJobId, preview));

        int skippedCount = (int) preview.foundCount() - idsToInsert.size();

        return new ConversionResult(preview.sourcePlaylistId(), destinationPlaylistId,
                preview.matches(), idsToInsert.size(), skippedCount);
    }

    private long registerCreatedDestination(String sourcePlaylistId, String destinationPlaylistId){
        try {
            return destinationRegistry.registerDestination(sourcePlaylistId, destinationPlaylistId);

        }catch (SyncStateStorageException e){
            throw new SyncStateStorageException("A playlist foi criada no YouTube com o ID "
                    + destinationPlaylistId
                    + ", mas não foi possível salvar o vínculo local. "
                    + "Nenhum vídeo foi inserido nesta execução. "
                    + "Use esse destino ao tentar novamente.", e);
        }
    }
}
