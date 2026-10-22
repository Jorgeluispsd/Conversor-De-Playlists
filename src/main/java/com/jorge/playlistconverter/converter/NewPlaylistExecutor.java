package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.enums.PlaylistVisibility;
import com.jorge.playlistconverter.service.MusicService;

import java.util.List;
import java.util.Objects;

public class NewPlaylistExecutor {

    private final MusicService destination;
    private final PlaylistConversionValidator validator;

    public NewPlaylistExecutor(MusicService destination, PlaylistConversionValidator validator) {
        this.destination = Objects.requireNonNull(destination);
        this.validator = Objects.requireNonNull(validator);
    }

    public ConversionResult execute(PlaylistPreview preview, String destinationName,
                                    PlaylistVisibility visibility){
        validator.validateForNewPlaylist(preview, destinationName, visibility);

        List<String> idsToInsert = preview.newTrackIds();

        String destinationPlaylistId = destination.createPlaylist(destinationName.strip(),
                visibility);

        destination.addTracks(destinationPlaylistId, idsToInsert);

        int skippedCount = (int) preview.foundCount() - idsToInsert.size();

        return new ConversionResult(preview.sourcePlaylistId(), destinationPlaylistId,
                preview.matches(), idsToInsert.size(), skippedCount);
    }
}
