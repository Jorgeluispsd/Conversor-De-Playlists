package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.errors.youtube.YoutubePlaylistReadException;
import com.jorge.playlistconverter.errors.youtube.YoutubeTrackInsertionException;
import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExistingPlaylistConversionTest {

    private MusicService source;
    private MusicService destination;
    private TrackMatcher matcher;
    private PlaylistConverter converter;

    private final Song firstSong =
            new Song("source-A", "Song A", "Artist A", 200000);

    private final Song secondSong =
            new Song("source-B", "Song B", "Artist B", 180000);

    private final Song thirdSong =
            new Song("source-C", "Song C", "Artist C", 190000);

    @BeforeEach
    void setUp() {
        source = mock(MusicService.class);
        destination = mock(MusicService.class);
        matcher = mock(TrackMatcher.class);

        converter = new PlaylistConverter(source, destination, matcher);
    }

    @Test
    void shouldInsertOnlyMissingVideosInSourceOrder() {
        Song existingVideo =
                new Song("video-A", "Song A", "Artist A", 0);

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of(existingVideo));

        MatchResult firstMatch =
                new MatchResult(firstSong, "video-A", "Song A", 0.95);

        MatchResult secondMatch =
                new MatchResult(secondSong, "video-B", "Song B", 0.90);

        MatchResult thirdMatch =
                new MatchResult(thirdSong, "video-C", "Song C", 0.92);

        prepareAnalysis(firstMatch, secondMatch, thirdMatch);

        ConversionResult result = converter.convertIntoExistingPlaylist(
                "source-playlist", "destination-playlist");

        assertEquals("source-playlist", result.sourcePlaylistId());
        assertEquals("destination-playlist", result.destinationPlaylistId());
        assertEquals(
                List.of(firstMatch, secondMatch, thirdMatch),
                result.matches());

        assertEquals(2, result.insertedCount());
        assertEquals(1, result.skippedCount());

        verify(destination).addTracks(
                "destination-playlist", List.of("video-B", "video-C"));

        verify(destination, never()).createPlaylist(anyString());

        var order = inOrder(destination, source);
        order.verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");
        order.verify(source).getPlaylistTracks("source-playlist");
    }

    @Test
    void shouldInsertRepeatedCandidateOnlyOnce() {
        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of());

        prepareAnalysis(
                new MatchResult(firstSong, "same-video", "Same song", 0.95),
                new MatchResult(secondSong, "same-video", "Same song", 0.90));

        ConversionResult result = converter.convertIntoExistingPlaylist(
                "source-playlist", "destination-playlist");

        assertEquals(1, result.insertedCount());
        assertEquals(1, result.skippedCount());

        verify(destination).addTracks(
                "destination-playlist", List.of("same-video"));

        verify(destination, never()).createPlaylist(anyString());
    }

    @Test
    void shouldAvoidWritingWhenEveryVideoAlreadyExists() {
        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of(
                        new Song("video-A", "Song A", "Artist A", 0)));

        prepareAnalysis(
                new MatchResult(firstSong, "video-A", "Song A", 0.95));

        ConversionResult result = converter.convertIntoExistingPlaylist(
                "source-playlist", "destination-playlist");

        assertEquals(0, result.insertedCount());
        assertEquals(1, result.skippedCount());

        verify(destination, never()).addTracks(anyString(), anyList());
        verify(destination, never()).createPlaylist(anyString());
    }

    @Test
    void shouldKeepUnmatchedSongWithoutCountingItAsDuplicate() {
        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of());

        MatchResult missingMatch =
                new MatchResult(firstSong, null, null, 0.0);

        MatchResult foundMatch =
                new MatchResult(secondSong, "video-B", "Song B", 0.90);

        prepareAnalysis(missingMatch, foundMatch);

        ConversionResult result = converter.convertIntoExistingPlaylist(
                "source-playlist", "destination-playlist");

        assertEquals(List.of(missingMatch, foundMatch), result.matches());
        assertEquals(1, result.insertedCount());
        assertEquals(0, result.skippedCount());

        verify(destination).addTracks(
                "destination-playlist", List.of("video-B"));

        verify(destination, never()).createPlaylist(anyString());
    }

    @Test
    void shouldRejectInvalidIdsBeforeReading() {
        for (String invalidId : new String[]{null, "", "   "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> converter.convertIntoExistingPlaylist(
                            invalidId, "destination-playlist"));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> converter.convertIntoExistingPlaylist(
                            "source-playlist", invalidId));
        }

        verifyNoInteractions(source, destination, matcher);
    }

    @Test
    void shouldStopBeforeAnalysisWhenDestinationReadFails() {
        YoutubePlaylistReadException original = new YoutubePlaylistReadException(
                        "Falha de leitura simulada.",
                        new IOException("Falha simulada"));

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenThrow(original);

        YoutubePlaylistReadException result = assertThrows(
                YoutubePlaylistReadException.class,
                () -> converter.convertIntoExistingPlaylist(
                        "source-playlist", "destination-playlist"));

        assertSame(original, result);

        verifyNoInteractions(source, matcher);

        verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");
        verifyNoMoreInteractions(destination);
    }

    @Test
    void shouldPreserveInsertionFailureWithoutCreatingAnotherPlaylist() {
        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of());

        prepareAnalysis(new MatchResult(
                firstSong, "video-A", "Song A", 0.95));

        YoutubeTrackInsertionException original = new YoutubeTrackInsertionException(
                        "Falha de inserção simulada.");

        doThrow(original).when(destination).addTracks(
                "destination-playlist", List.of("video-A"));

        YoutubeTrackInsertionException result = assertThrows(
                YoutubeTrackInsertionException.class,
                () -> converter.convertIntoExistingPlaylist("source-playlist",
                        "destination-playlist"));

        assertSame(original, result);

        verify(destination, times(1)).addTracks(
                "destination-playlist", List.of("video-A"));

        verify(destination, never()).createPlaylist(anyString());
    }

    @Test
    void shouldSkipExistingMatchWithoutSearchingOrWriting() {
        Song existingSong = new Song(
                "existing-video", "Artist A - Song A (Official Audio)",
                "Artist A", 0);

        List<Song> existingSongs = List.of(existingSong);

        MatchResult existingMatch = new MatchResult(
                firstSong, "existing-video",
                existingSong.title(), 0.90);

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(existingSongs);

        when(source.getPlaylistTracks("source-playlist"))
                .thenReturn(List.of(firstSong));

        when(matcher.findExistingMatch(firstSong, existingSongs))
                .thenReturn(existingMatch);

        ConversionResult result = converter.convertIntoExistingPlaylist(
                "source-playlist", "destination-playlist");

        assertEquals(0, result.insertedCount());
        assertEquals(1, result.skippedCount());
        assertEquals(List.of(existingMatch), result.matches());

        verify(matcher).findExistingMatch(firstSong, existingSongs);
        verifyNoMoreInteractions(matcher);

        verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");
        verifyNoMoreInteractions(destination);
    }

    private void prepareAnalysis(MatchResult... matches) {
        List<Song> songs = Arrays.stream(matches)
                .map(MatchResult::sourceSong).toList();

        when(source.getPlaylistTracks("source-playlist"))
                .thenReturn(songs);

        for (MatchResult match : matches) {
            when(matcher.findExistingMatch(
                    eq(match.sourceSong()),
                    anyList()))
                    .thenReturn(new MatchResult(
                            match.sourceSong(),
                            null, null, 0.0));

            List<Song> candidates = match.found()
                    ? List.of(new Song(
                    match.matchedId(),
                    match.matchedTitle(),
                    match.sourceSong().artist(),
                    0))
                    : List.of();

            when(destination.searchCandidates(match.sourceSong())).thenReturn(candidates);

            when(matcher.findBestMatch(match.sourceSong(), candidates)).thenReturn(match);
        }
    }
}
