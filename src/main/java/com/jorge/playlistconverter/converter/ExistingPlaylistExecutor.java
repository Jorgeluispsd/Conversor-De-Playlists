package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.errors.conversion.PlaylistPreviewChangedException;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ExistingPlaylistExecutor {

    private final MusicService destination;
    private final PlaylistConversionValidator validator;
    private final PlaylistDestinationRegistry destinationRegistry;
    private final SyncRunTracker runTracker;

    public ExistingPlaylistExecutor(MusicService destination, PlaylistConversionValidator validator,
                                    PlaylistDestinationRegistry destinationRegistry, SyncRunTracker runTracker) {
        this.destination = Objects.requireNonNull(destination);
        this.validator = Objects.requireNonNull(validator);
        this.destinationRegistry= Objects.requireNonNull(destinationRegistry);
        this.runTracker = Objects.requireNonNull(runTracker);
    }

    public ConversionResult execute(PlaylistPreview preview){
        validator.validateForExistingPlaylist(preview);

        Set<String> currentDestinationIds = destination.getPlaylistTracksAlreadyAdded(
                preview.destinationPlaylistId()).stream().map(Song::id).collect(Collectors.toSet());

        validateDestinationSnapshot(preview, currentDestinationIds);

        long syncJobId = destinationRegistry
                .registerDestination(preview.sourcePlaylistId(), preview.destinationPlaylistId());

        List<String> idsToInsert = preview.newTrackIds().stream()
                .filter(id -> !currentDestinationIds.contains(id)).toList();

        return runTracker.execute(syncJobId, () -> insertTracks(preview, idsToInsert));
    }

    private ConversionResult insertTracks(PlaylistPreview preview, List<String> idsToInsert){
        if (!idsToInsert.isEmpty()){
            destination.addTracks(preview.destinationPlaylistId(), idsToInsert);
        }

        int skippedCount = (int) preview.foundCount() - idsToInsert.size();

        return new ConversionResult(preview.sourcePlaylistId(), preview.destinationPlaylistId(),
                preview.matches(), idsToInsert.size(), skippedCount);
    }

    private void validateDestinationSnapshot(PlaylistPreview preview, Set<String> currentDestinationIds){
        if (!currentDestinationIds.containsAll(preview.existingDestinationIds())){
            throw new PlaylistPreviewChangedException(
                    "Itens da playlist de destino foram removidos "
                            + "desde a análise. Gere uma nova prévia "
                            + "antes de executar a conversão.");
        }
    }
}
