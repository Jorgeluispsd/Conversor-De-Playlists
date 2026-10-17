package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.errors.spotify.SpotifyPlaylistReadException;
import com.jorge.playlistconverter.errors.youtube.YoutubeSearchException;
import com.jorge.playlistconverter.errors.youtube.YoutubeTrackInsertionException;
import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlaylistConverterTest {
    private MusicService source;
    private MusicService destination;
    private TrackMatcher matcher;
    private PlaylistConverter converter;

    private final Song firstSong =
            new Song("source-A", "Song A", "Artist A", 200000);

    private final Song secondSong =
            new Song("source-B", "Song B", "Artist B", 180000);

    @BeforeEach
    void setUp() {
        source = mock(MusicService.class);
        destination = mock(MusicService.class);
        matcher = mock(TrackMatcher.class);

        converter = new PlaylistConverter(source, destination, matcher);
    }

    @Test
    void shouldAnalyzeSongsInSourceOrderWithoutWriting() {
        Song firstCandidate =
                new Song("video-A", "Song A", "Artist A", 0);

        Song secondCandidate =
                new Song("video-B", "Song B", "Artist B", 0);

        List<Song> firstCandidates = List.of(firstCandidate);
        List<Song> secondCandidates = List.of(secondCandidate);

        MatchResult firstMatch =
                new MatchResult(firstSong, "video-A", "Song A", 0.95);

        MatchResult secondMatch =
                new MatchResult(secondSong, "video-B", "Song B", 0.90);

        when(source.getPlaylistTracks("source-playlist"))
                .thenReturn(List.of(firstSong, secondSong));

        when(destination.searchCandidates(firstSong))
                .thenReturn(firstCandidates);

        when(destination.searchCandidates(secondSong))
                .thenReturn(secondCandidates);

        when(matcher.findBestMatch(firstSong, firstCandidates))
                .thenReturn(firstMatch);

        when(matcher.findBestMatch(secondSong, secondCandidates))
                .thenReturn(secondMatch);

        List<MatchResult> results =
                converter.analyzePlaylist("source-playlist");

        assertEquals(List.of(firstMatch, secondMatch), results);

        var order = inOrder(source, destination, matcher);

        order.verify(source).getPlaylistTracks("source-playlist");
        order.verify(destination).searchCandidates(firstSong);
        order.verify(matcher).findBestMatch(firstSong, firstCandidates);
        order.verify(destination).searchCandidates(secondSong);
        order.verify(matcher).findBestMatch(secondSong, secondCandidates);

        verifyNoMoreInteractions(source, destination, matcher);
    }

    @Test
    void shouldKeepSongWithoutMatchAndContinueAnalysis() {
        List<Song> noCandidates = List.of();

        List<Song> secondCandidates = List.of(
                new Song("video-B", "Song B", "Artist B", 0));

        MatchResult missingMatch =
                new MatchResult(firstSong, null, null, 0.0);

        MatchResult secondMatch =
                new MatchResult(secondSong, "video-B", "Song B", 0.90);

        when(source.getPlaylistTracks("source-playlist"))
                .thenReturn(List.of(firstSong, secondSong));

        when(destination.searchCandidates(firstSong))
                .thenReturn(noCandidates);

        when(destination.searchCandidates(secondSong))
                .thenReturn(secondCandidates);

        when(matcher.findBestMatch(firstSong, noCandidates))
                .thenReturn(missingMatch);

        when(matcher.findBestMatch(secondSong, secondCandidates))
                .thenReturn(secondMatch);

        List<MatchResult> results =
                converter.analyzePlaylist("source-playlist");

        assertEquals(List.of(missingMatch, secondMatch), results);
        assertFalse(results.get(0).found());
        assertTrue(results.get(1).found());

        verify(destination).searchCandidates(secondSong);
    }

    @Test
    void shouldReturnEmptyAnalysisForEmptyPlaylist() {
        when(source.getPlaylistTracks("source-playlist"))
                .thenReturn(List.of());

        List<MatchResult> results =
                converter.analyzePlaylist("source-playlist");

        assertTrue(results.isEmpty());

        verify(source).getPlaylistTracks("source-playlist");
        verifyNoInteractions(destination, matcher);
    }

    @Test
    void shouldRejectInvalidPlaylistIdBeforeReading() {
        for (String playlistId : new String[]{null, "", "   "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> converter.analyzePlaylist(playlistId));
        }

        verifyNoInteractions(source, destination, matcher);
    }

    @Test
    void shouldPreserveSourceReadFailure() {
        SpotifyPlaylistReadException original =
                new SpotifyPlaylistReadException("Falha de leitura simulada.",
                        new IOException("Falha simulada"));

        when(source.getPlaylistTracks("source-playlist"))
                .thenThrow(original);

        SpotifyPlaylistReadException result = assertThrows(
                SpotifyPlaylistReadException.class,
                () -> converter.analyzePlaylist("source-playlist"));

        assertSame(original, result);

        verifyNoInteractions(destination, matcher);
    }

    @Test
    void shouldStopAnalysisAfterSearchFailure() {
        YoutubeSearchException original =
                new YoutubeSearchException("Falha de busca simulada.",
                        new IOException("Falha simulada"));

        when(source.getPlaylistTracks("source-playlist"))
                .thenReturn(List.of(firstSong, secondSong));

        when(destination.searchCandidates(firstSong))
                .thenThrow(original);

        YoutubeSearchException result = assertThrows(
                YoutubeSearchException.class,
                () -> converter.analyzePlaylist("source-playlist"));

        assertSame(original, result);

        verify(destination).searchCandidates(firstSong);
        verify(destination, never()).searchCandidates(secondSong);
        verifyNoInteractions(matcher);
    }

    @Test
    void shouldCreatePlaylistAndInsertOnlyFoundMatches() {
        List<Song> firstCandidates = List.of(
                new Song("video-A", "Song A", "Artist A", 0));

        MatchResult firstMatch =
                new MatchResult(firstSong, "video-A", "Song A", 0.95);

        MatchResult missingMatch =
                new MatchResult(secondSong, null, null, 0.0);

        when(source.getPlaylistTracks("source-playlist"))
                .thenReturn(List.of(firstSong, secondSong));

        when(destination.searchCandidates(firstSong))
                .thenReturn(firstCandidates);

        when(destination.searchCandidates(secondSong))
                .thenReturn(List.of());

        when(matcher.findBestMatch(firstSong, firstCandidates))
                .thenReturn(firstMatch);

        when(matcher.findBestMatch(secondSong, List.of()))
                .thenReturn(missingMatch);

        when(destination.createPlaylist("Minha playlist"))
                .thenReturn("destination-playlist");

        ConversionResult result = converter.convertPlaylist(
                "source-playlist",
                "Minha playlist");

        assertEquals("source-playlist", result.sourcePlaylistId());
        assertEquals("destination-playlist", result.destinationPlaylistId());
        assertEquals(List.of(firstMatch, missingMatch), result.matches());
        assertEquals(1, result.insertedCount());

        var order = inOrder(destination);

        order.verify(destination).searchCandidates(firstSong);
        order.verify(destination).searchCandidates(secondSong);
        order.verify(destination).createPlaylist("Minha playlist");

        order.verify(destination).addTracks(
                "destination-playlist", List.of("video-A"));

        verifyNoMoreInteractions(destination);
    }

    @Test
    void shouldRejectInvalidDestinationNameBeforeAnalysis() {
        for (String name : new String[]{null, "", "   "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> converter.convertPlaylist("source-playlist", name));
        }

        verifyNoInteractions(source, destination, matcher);
    }

    @Test
    void shouldNotCreatePlaylistWhenAnalysisFails() {
        YoutubeSearchException original = new YoutubeSearchException("Falha simulada.",
                new IOException("Falha de comunicação simulada"));

        when(source.getPlaylistTracks("source-playlist"))
                .thenReturn(List.of(firstSong));

        when(destination.searchCandidates(firstSong))
                .thenThrow(original);

        YoutubeSearchException result = assertThrows(
                YoutubeSearchException.class,
                () -> converter.convertPlaylist(
                        "source-playlist",
                        "Minha playlist"));

        assertSame(original, result);

        verify(destination).searchCandidates(firstSong);
        verifyNoMoreInteractions(destination);
    }

    @Test
    void shouldPreserveInsertionFailureWithoutRetrying() {
        List<Song> candidates = List.of(
                new Song("video-A", "Song A", "Artist A", 0));

        MatchResult match =
                new MatchResult(firstSong, "video-A", "Song A", 0.95);

        YoutubeTrackInsertionException original =
                new YoutubeTrackInsertionException("Falha de inserção simulada.");

        when(source.getPlaylistTracks("source-playlist"))
                .thenReturn(List.of(firstSong));

        when(destination.searchCandidates(firstSong))
                .thenReturn(candidates);

        when(matcher.findBestMatch(firstSong, candidates))
                .thenReturn(match);

        when(destination.createPlaylist("Minha playlist"))
                .thenReturn("destination-playlist");

        doThrow(original).when(destination).addTracks(
                "destination-playlist",
                List.of("video-A"));

        YoutubeTrackInsertionException result = assertThrows(
                YoutubeTrackInsertionException.class,
                () -> converter.convertPlaylist(
                        "source-playlist",
                        "Minha playlist"));

        assertSame(original, result);

        verify(destination, times(1)).createPlaylist("Minha playlist");

        verify(destination, times(1)).addTracks(
                "destination-playlist",
                List.of("video-A"));
    }
}
