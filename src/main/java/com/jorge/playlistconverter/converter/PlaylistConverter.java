package com.jorge.playlistconverter.converter;

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
        PlaylistPreview preview = previewIntoExistingPlaylist(sourcePlaylistId,
                destinationPlaylistId);

        List<String> newIds = preview.newTrackIds();

        if (!newIds.isEmpty())
            destination.addTracks(destinationPlaylistId, newIds);

        int skippedCount = (int) preview.foundCount() - newIds.size();

        return new ConversionResult(sourcePlaylistId, destinationPlaylistId,
                preview.matches(), newIds.size(), skippedCount);
    }

    public PlaylistPreview previewPlaylist(String sourcePlaylistId) {
        List<MatchResult> matches = analyzePlaylist(sourcePlaylistId);

        return new PlaylistPreview(sourcePlaylistId, matches);
    }

    public PlaylistPreview previewIntoExistingPlaylist(String sourcePlaylistId,
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
}
