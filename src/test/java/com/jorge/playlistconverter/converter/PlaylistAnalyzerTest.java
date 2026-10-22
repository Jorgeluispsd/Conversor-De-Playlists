package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.errors.spotify.SpotifyPlaylistReadException;
import com.jorge.playlistconverter.errors.youtube.YoutubeSearchException;
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

class PlaylistAnalyzerTest {

    private MusicService source;
    private MusicService destination;
    private TrackMatcher matcher;
    private PlaylistAnalyzer analyzer;

    private final Song firstSong =
            new Song("source-A", "Song A", "Artist A", 200000);

    private final Song secondSong =
            new Song("source-B", "Song B", "Artist B", 180000);

    @BeforeEach
    void setUp() {
        source = mock(MusicService.class);
        destination = mock(MusicService.class);
        matcher = mock(TrackMatcher.class);

        analyzer = new PlaylistAnalyzer(source, destination, matcher);
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
                analyzer.analyze("source-playlist");

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
                analyzer.analyze("source-playlist");

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
                analyzer.analyze("source-playlist");

        assertTrue(results.isEmpty());

        verify(source).getPlaylistTracks("source-playlist");
        verifyNoInteractions(destination, matcher);
    }

    @Test
    void shouldRejectInvalidPlaylistIdBeforeReading() {
        for (String playlistId : new String[]{null, "", "   "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> analyzer.analyze(playlistId));
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
                () -> analyzer.analyze("source-playlist"));

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
                () -> analyzer.analyze("source-playlist"));

        assertSame(original, result);

        verify(destination).searchCandidates(firstSong);
        verify(destination, never()).searchCandidates(secondSong);
        verifyNoInteractions(matcher);
    }

}
