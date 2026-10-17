package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        if (sourcePlaylistId == null || sourcePlaylistId.isBlank()) {
            throw new IllegalArgumentException("O ID da playlist de origem deve ser informado.");
        }

        List<Song> songs = source.getPlaylistTracks(sourcePlaylistId);

        List<MatchResult> results = new ArrayList<>();

        for (Song song : songs){

            results.add(analyzeSong(song));
        }

        return List.copyOf(results);
    }

    private MatchResult analyzeSong(Song song){
        List<Song> candidates = destination.searchCandidates(song);

        return matcher.findBestMatch(song, candidates);
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

        return new ConversionResult(sourcePlaylistId, destinationPlaylistId, matches, destinationTrackIds.size());
    }


}
