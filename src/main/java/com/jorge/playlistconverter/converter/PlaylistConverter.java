package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.enums.PlaylistVisibility;
import com.jorge.playlistconverter.errors.conversion.PlaylistPreviewChangedException;
import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class PlaylistConverter {
    private final MusicService source;
    private final MusicService destination;
    private final TrackMatcher matcher;

    public PlaylistConverter(MusicService source, MusicService destination, TrackMatcher matcher) {
        this.source = Objects.requireNonNull(source);
        this.destination = Objects.requireNonNull(destination);
        this.matcher = Objects.requireNonNull(matcher);
    }

    public List<MatchResult> analyzePlaylist(String sourcePlaylistId) {
        return analyzePlaylist(sourcePlaylistId, List.of());

    }

    private List<MatchResult> analyzePlaylist(String sourcePlaylistId, List<Song> existingSongs){

        if (sourcePlaylistId == null || sourcePlaylistId.isBlank()) {
            throw new IllegalArgumentException("O ID da playlist de origem deve ser informado.");
        }

        List<Song> songs = source.getPlaylistTracks(sourcePlaylistId);

        List<MatchResult> results = new ArrayList<>();

        for (Song song : songs){
            results.add(analyzeSong(song, existingSongs));
        }

        return List.copyOf(results);
    }

    private MatchResult analyzeSong(Song song){
        List<Song> candidates = destination.searchCandidates(song);

        log.debug("Origem da análise: {}", song);

        for (int index = 0; index < candidates.size(); index++) {

            log.debug("Candidato {}: {}", index + 1, candidates.get(index));
        }

        MatchResult result = matcher.findBestMatch(song, candidates);

        log.debug("Resultado da análise: {}", result);

        return result;
    }

    private MatchResult analyzeSong(Song song, List<Song> existingSongs){

        if (existingSongs.isEmpty()){
            return analyzeSong(song);
        }

        MatchResult existingMatch = matcher.findExistingMatch(song, existingSongs);

        if (existingMatch.found()){
            log.debug(
                    "Correspondência já presente no destino: {}",
                    existingMatch);

            return existingMatch;
        }

        return analyzeSong(song);
    }

    public ConversionResult convertPlaylist(String sourcePlaylistId, String destinationPlaylistName){
        if (destinationPlaylistName == null || destinationPlaylistName.isBlank()){
            throw new IllegalArgumentException("O nome da playlist de destino deve ser informado.");
        }

        List<MatchResult> matches = analyzePlaylist(sourcePlaylistId);

        List<String> destinationTrackIds = matches.stream()
                .filter(MatchResult::found)
                .map(MatchResult::matchedId)
                .toList();

        String destinationPlaylistId =
                destination.createPlaylist(destinationPlaylistName);

        destination.addTracks(destinationPlaylistId, destinationTrackIds);

        return new ConversionResult(sourcePlaylistId, destinationPlaylistId, matches,
                destinationTrackIds.size(), 0);
    }

    public ConversionResult convertIntoExistingPlaylist(String sourcePlaylistId,
                                                        String destinationPlaylistId){
        PlaylistPreview preview = previewExistingPlaylist(sourcePlaylistId,
                destinationPlaylistId);

        return executeExistingPlaylistPreview(preview);
    }

    public PlaylistPreview previewNewPlaylist(String sourcePlaylistId) {
        List<MatchResult> matches = analyzePlaylist(sourcePlaylistId);

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

        List<MatchResult> matches = analyzePlaylist(sourcePlaylistId, existingSongs);

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
        Objects.requireNonNull(preview, "A prévia deve ser informada.");

        validatePlaylistIds(preview.sourcePlaylistId(), preview.destinationPlaylistId());

        Set<String> currentDestinationIds = destination.getPlaylistTracksAlreadyAdded(
                preview.destinationPlaylistId()).stream().map(Song::id).collect(Collectors.toSet());

        if (!currentDestinationIds.containsAll(preview.existingDestinationIds())){
            throw new PlaylistPreviewChangedException(
                    "Itens da playlist de destino foram removidos "
                            + "desde a análise. Gere uma nova prévia "
                            + "antes de executar a conversão.");
        }

        List<String> idsToInsert = preview.newTrackIds().stream()
                .filter(id -> !currentDestinationIds.contains(id))
                .collect(Collectors.toList());

        if (!idsToInsert.isEmpty()){
            destination.addTracks(preview.destinationPlaylistId(), idsToInsert);
        }

        int skippedCount = (int) preview.foundCount() - idsToInsert.size();

        return new ConversionResult(preview.sourcePlaylistId(), preview.destinationPlaylistId(),
                preview.matches(), idsToInsert.size(), skippedCount);
    }

    public ConversionResult executeNewPlaylistPreview(PlaylistPreview preview,
                                                      String destinationPlaylistName){

        return executeNewPlaylistPreview(preview, destinationPlaylistName, PlaylistVisibility.PRIVATE);
    }

    public ConversionResult executeNewPlaylistPreview(PlaylistPreview preview,
                                                      String destinationPlaylistName,
                                                      PlaylistVisibility visibility){
        validateNewPlaylistPreview(preview, destinationPlaylistName, visibility);

        List<String> idsToInsert = requireNewTrackIds(preview);

        String destinationPlaylistId = destination.createPlaylist(
                destinationPlaylistName.strip(), visibility);

        destination.addTracks(destinationPlaylistId, idsToInsert);

        int skippedCount = (int) preview.foundCount() - idsToInsert.size();

        return new ConversionResult(preview.sourcePlaylistId(), destinationPlaylistId,
                preview.matches(), idsToInsert.size(), skippedCount);
    }


    private void validateNewPlaylistPreview(PlaylistPreview preview, String destinationPlaylistName,
                                            PlaylistVisibility visibility){

        Objects.requireNonNull(preview, "A prévia deve ser informada.");
        Objects.requireNonNull(
                visibility,
                "A visibilidade da playlist deve ser informada.");

        requireNonBlank(preview.sourcePlaylistId(), "O ID da playlist de origem deve ser informado.");

        requireNonBlank(destinationPlaylistName, "O nome da playlist de destino deve ser informado.");

        if (preview.destinationPlaylistId() != null
                || !preview.existingDestinationIds().isEmpty()){
            throw new IllegalArgumentException(
                    "A prévia deve ser destinada à criação de uma nova playlist.");
        }
    }

    private void requireNonBlank(String value, String message){
        if (value == null || value.isBlank()){
            throw new IllegalArgumentException(message);
        }
    }

    private List<String> requireNewTrackIds(PlaylistPreview preview){
        List<String> ids = preview.newTrackIds();

        if(ids.isEmpty()){
            throw new IllegalArgumentException(
                    "A prévia não possui músicas encontradas para adicionar.");
        }

        return ids;
    }
}
