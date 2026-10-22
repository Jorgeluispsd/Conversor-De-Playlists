package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.enums.PlaylistVisibility;

import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PlaylistConverter {
    private final PlaylistAnalyzer analyzer;
    private final MusicService destination;
    private final NewPlaylistExecutor newPlaylistExecutor;
    private final ExistingPlaylistExecutor existingPlaylistExecutor;

    public PlaylistConverter(PlaylistAnalyzer analyzer, MusicService destination,
                             NewPlaylistExecutor newPlaylistExecutor,
                             ExistingPlaylistExecutor existingPlaylistExecutor) {
        this.analyzer = Objects.requireNonNull(analyzer);
        this.destination = Objects.requireNonNull(destination);
        this.newPlaylistExecutor = Objects.requireNonNull(newPlaylistExecutor);
        this.existingPlaylistExecutor = Objects.requireNonNull(existingPlaylistExecutor);
    }


    public PlaylistPreview previewNewPlaylist(String sourcePlaylistId) {
        List<MatchResult> matches = analyzer.analyze(sourcePlaylistId);

        return new PlaylistPreview(sourcePlaylistId, matches);
    }

    public PlaylistPreview previewExistingPlaylist(String sourcePlaylistId,
                                                   String destinationPlaylistId){
        validatePlaylistIds(sourcePlaylistId, destinationPlaylistId);

        List<Song> existingSongs =
                destination.getPlaylistTracksAlreadyAdded(destinationPlaylistId);

        Set<String> existingIds = existingSongs.stream()
                .map(Song::id)
                .collect(Collectors.toSet());

        List<MatchResult> matches = analyzer.analyze(sourcePlaylistId, existingSongs);

        return new PlaylistPreview(sourcePlaylistId, destinationPlaylistId,
                matches, existingIds);
    }

    private void validatePlaylistIds(String sourcePlaylistId, String destinationPlaylistId){
        if (sourcePlaylistId == null || sourcePlaylistId.isBlank()){
            throw new IllegalArgumentException(
                    "O ID da playlist de origem deve ser informado.");
        }

        if (destinationPlaylistId == null || destinationPlaylistId.isBlank()){
            throw new IllegalArgumentException(
                    "O ID da playlist de destino deve ser informado.");
        }
    }

    public ConversionResult executeExistingPlaylistPreview(PlaylistPreview preview){

        return existingPlaylistExecutor.execute(preview);
    }

    public ConversionResult executeNewPlaylistPreview(PlaylistPreview preview,
                                                      String destinationPlaylistName){

        return executeNewPlaylistPreview(preview, destinationPlaylistName, PlaylistVisibility.PRIVATE);
    }

    public ConversionResult executeNewPlaylistPreview(PlaylistPreview preview,
                                                      String destinationPlaylistName,
                                                      PlaylistVisibility visibility){

        return newPlaylistExecutor.execute(preview, destinationPlaylistName, visibility);
    }
}
