package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.converter.execution.ExistingPlaylistExecutor;
import com.jorge.playlistconverter.converter.execution.NewPlaylistExecutor;
import com.jorge.playlistconverter.converter.history.PlaylistDestinationRegistry;
import com.jorge.playlistconverter.converter.history.SyncRunTracker;
import com.jorge.playlistconverter.converter.history.TrackSyncRecorder;
import com.jorge.playlistconverter.converter.preview.PlaylistPreview;
import com.jorge.playlistconverter.errors.youtube.YoutubePlaylistReadException;
import com.jorge.playlistconverter.errors.youtube.YoutubeSearchException;
import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;
import com.jorge.playlistconverter.state.SyncStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExistingPlaylistPreviewTest {

    private MusicService source;
    private MusicService destination;
    private TrackMatcher matcher;
    private PlaylistConverter converter;

    @BeforeEach
    void setUp() {
        source = mock(MusicService.class);
        destination = mock(MusicService.class);
        matcher = mock(TrackMatcher.class);

        PlaylistAnalyzer analyzer = new PlaylistAnalyzer(source, destination, matcher);

        SyncStateStore store = mock(SyncStateStore.class);
        SyncRunTracker runTracker = new SyncRunTracker(store);
        TrackSyncRecorder trackSyncRecorder = new TrackSyncRecorder(store);

        PlaylistConversionValidator validator =
                new PlaylistConversionValidator();

        PlaylistDestinationRegistry destinationRegistry =
                mock(PlaylistDestinationRegistry.class);


        NewPlaylistExecutor newPlaylistExecutor =
                new NewPlaylistExecutor(destination, validator, destinationRegistry,
                        runTracker, trackSyncRecorder);

        ExistingPlaylistExecutor existingPlaylistExecutor =
                new ExistingPlaylistExecutor(destination, validator, destinationRegistry,
                        runTracker, trackSyncRecorder);

        converter = new PlaylistConverter(analyzer, destination,
                newPlaylistExecutor, existingPlaylistExecutor, validator);
    }

    @Test
    void shouldSummarizeExistingNewRepeatedAndUnmatchedSongsWithoutWriting() {
        MatchResult existing = match("source-A", "video-A");
        MatchResult firstNew = match("source-B", "video-B");
        MatchResult repeated = match("source-C", "video-B");
        MatchResult missing = match("source-D", null);
        MatchResult secondNew = match("source-E", "video-E");

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of(
                        new Song("video-A", "Existing song", "Artist", 0),
                        new Song("unrelated-video", "Other song", "Artist", 0)));

        prepareAnalysis(existing, firstNew, repeated, missing, secondNew);

        PlaylistPreview result = converter.previewExistingPlaylist(
                "source-playlist",
                "destination-playlist");

        assertEquals("source-playlist", result.sourcePlaylistId());
        assertEquals("destination-playlist", result.destinationPlaylistId());

        assertEquals(5, result.totalCount());
        assertEquals(4L, result.foundCount());
        assertEquals(1L, result.alreadyPresentCount());
        assertEquals(2, result.newTrackCount());
        assertEquals(1L, result.repeatedCandidateCount());

        assertEquals(
                List.of("video-B", "video-E"),
                result.newTrackIds());

        assertEquals(
                List.of(missing.sourceSong()),
                result.notFoundSongs());

        assertEquals(
                List.of(existing, firstNew, repeated, missing, secondNew),
                result.matches());

        verify(destination, never()).createPlaylist(anyString());
        verify(destination, never()).addTracks(anyString(), anyList(), any());
    }

    @Test
    void shouldCountRepeatedExistingMatchesAsAlreadyPresent() {
        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of(
                        new Song("video-A", "Existing song", "Artist", 0)));

        prepareAnalysis(
                match("source-A", "video-A"),
                match("source-B", "video-A"));

        PlaylistPreview result = converter.previewExistingPlaylist(
                "source-playlist", "destination-playlist");

        assertEquals(2L, result.foundCount());
        assertEquals(2L, result.alreadyPresentCount());
        assertEquals(0, result.newTrackCount());
        assertEquals(0L, result.repeatedCandidateCount());
        assertTrue(result.newTrackIds().isEmpty());

        verify(destination, never()).createPlaylist(anyString());
        verify(destination, never()).addTracks(anyString(), anyList(), any());
    }

    @Test
    void shouldReturnZeroCountsForEmptySource() {
        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of());

        when(source.getPlaylistTracks("source-playlist"))
                .thenReturn(List.of());

        PlaylistPreview result = converter.previewExistingPlaylist(
                "source-playlist", "destination-playlist");

        assertEquals(0, result.totalCount());
        assertEquals(0L, result.foundCount());
        assertEquals(0L, result.alreadyPresentCount());
        assertEquals(0, result.newTrackCount());
        assertEquals(0L, result.repeatedCandidateCount());
        assertTrue(result.notFoundSongs().isEmpty());

        verify(destination).getPlaylistTracksAlreadyAdded("destination-playlist");
        verifyNoMoreInteractions(destination);
        verifyNoInteractions(matcher);
    }

    @Test
    void shouldRejectInvalidIdsBeforeReading() {
        for (String invalidId : new String[]{null, "", "   "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> converter.previewExistingPlaylist(invalidId,
                            "destination-playlist"));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> converter.previewExistingPlaylist(
                            "source-playlist", invalidId));
        }

        verifyNoInteractions(source, destination, matcher);
    }

    @Test
    void shouldStopBeforeAnalysisWhenDestinationReadFails() {
        YoutubePlaylistReadException original =
                new YoutubePlaylistReadException(
                        "Falha de leitura simulada.", new IOException("Falha simulada"));

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenThrow(original);

        YoutubePlaylistReadException result = assertThrows(
                YoutubePlaylistReadException.class,
                () -> converter.previewExistingPlaylist(
                        "source-playlist", "destination-playlist"));

        assertSame(original, result);

        verifyNoInteractions(source, matcher);

        verify(destination).getPlaylistTracksAlreadyAdded("destination-playlist");
        verifyNoMoreInteractions(destination);
    }

    @Test
    void shouldPropagateSearchFailureInsteadOfReportingUnmatchedSong() {
        Song song = new Song("source-A", "Song A",
                "Artist A", 200000);

        YoutubeSearchException original = new YoutubeSearchException(
                "Falha de busca simulada.", new IOException("Falha simulada"));

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of());

        when(source.getPlaylistTracks("source-playlist")).thenReturn(List.of(song));

        when(destination.searchCandidates(song)).thenThrow(original);

        YoutubeSearchException result = assertThrows(
                YoutubeSearchException.class,
                () -> converter.previewExistingPlaylist(
                        "source-playlist", "destination-playlist"));

        assertSame(original, result);
        verifyNoInteractions(matcher);

        verify(destination).getPlaylistTracksAlreadyAdded("destination-playlist");
        verify(destination).searchCandidates(song);
        verifyNoMoreInteractions(destination);
    }

    @Test
    void shouldSearchOnlyForSongsNotMatchedInDestination() {
        Song firstSong = new Song("source-A", "Song A",
                "Artist A", 200000);

        Song secondSong = new Song("source-B", "Song B",
                "Artist B", 180000);

        Song existingSong = new Song("existing-video", "Artist A - Song A (Official Audio)",
                "Artist A", 0);

        List<Song> existingSongs = List.of(existingSong);

        List<Song> candidates = List.of(
                new Song("new-video", "Song B", "Artist B", 0));

        MatchResult existingMatch = new MatchResult(
                firstSong, "existing-video",
                existingSong.title(), 0.90);

        MatchResult missingExistingMatch =
                new MatchResult(secondSong, null, null, 0.0);

        MatchResult newMatch =
                new MatchResult(secondSong, "new-video", "Song B", 0.95);

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(existingSongs);

        when(source.getPlaylistTracks("source-playlist"))
                .thenReturn(List.of(firstSong, secondSong));

        when(matcher.findExistingMatch(firstSong, existingSongs))
                .thenReturn(existingMatch);

        when(matcher.findExistingMatch(secondSong, existingSongs))
                .thenReturn(missingExistingMatch);

        when(destination.searchCandidates(secondSong))
                .thenReturn(candidates);

        when(matcher.findBestMatch(secondSong, candidates))
                .thenReturn(newMatch);

        PlaylistPreview result = converter.previewExistingPlaylist(
                "source-playlist", "destination-playlist");

        assertEquals(List.of(existingMatch, newMatch), result.matches());
        assertEquals(2L, result.foundCount());
        assertEquals(1L, result.alreadyPresentCount());
        assertEquals(1, result.newTrackCount());
        assertEquals(List.of("new-video"), result.newTrackIds());

        verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");
        verify(destination).searchCandidates(secondSong);
        verifyNoMoreInteractions(destination);

        verify(matcher).findExistingMatch(firstSong, existingSongs);
        verify(matcher).findExistingMatch(secondSong, existingSongs);
        verify(matcher).findBestMatch(secondSong, candidates);
        verifyNoMoreInteractions(matcher);
    }

    private MatchResult match(String sourceId, String videoId) {
        Song song = new Song(
                sourceId,
                "Song " + sourceId,
                "Test Artist",
                200000);

        return videoId == null
                ? new MatchResult(song, null, null, 0.0)
                : new MatchResult(song, videoId, "Matched song", 0.95);
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

            when(destination.searchCandidates(match.sourceSong()))
                    .thenReturn(candidates);

            when(matcher.findBestMatch(match.sourceSong(), candidates))
                    .thenReturn(match);
        }
    }

}
