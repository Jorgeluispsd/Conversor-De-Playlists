package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.errors.conversion.PlaylistPreviewChangedException;
import com.jorge.playlistconverter.errors.youtube.YoutubePlaylistReadException;
import com.jorge.playlistconverter.errors.youtube.YoutubeTrackInsertionException;
import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExistingPlaylistPreviewExecutionTest {

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

        converter = new PlaylistConverter(analyzer, destination);
    }

    @Test
    void shouldExecutePlannedIdsWithoutRepeatingAnalysis() {
        PlaylistPreview preview = preview();

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of(
                        video("video-A"),
                        video("unrelated-video")));

        ConversionResult result =
                converter.executeExistingPlaylistPreview(preview);

        assertEquals("source-playlist", result.sourcePlaylistId());
        assertEquals("destination-playlist", result.destinationPlaylistId());
        assertEquals(preview.matches(), result.matches());
        assertEquals(2, result.insertedCount());
        assertEquals(1, result.skippedCount());

        var order = inOrder(destination);

        order.verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");

        order.verify(destination).addTracks(
                "destination-playlist",
                List.of("video-B", "video-C"));

        verifyNoMoreInteractions(destination);
        verifyNoInteractions(source, matcher);
    }

    @Test
    void shouldSkipVideoAddedAfterPreview() {
        PlaylistPreview preview = preview();

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of(
                        video("video-A"),
                        video("video-B")));

        ConversionResult result =
                converter.executeExistingPlaylistPreview(preview);

        assertEquals(1, result.insertedCount());
        assertEquals(2, result.skippedCount());

        verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");

        verify(destination).addTracks(
                "destination-playlist",
                List.of("video-C"));

        verifyNoMoreInteractions(destination);
        verifyNoInteractions(source, matcher);
    }

    @Test
    void shouldRejectPreviewWhenExistingVideoWasRemoved() {
        PlaylistPreview preview = preview();

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of());

        assertThrows(
                PlaylistPreviewChangedException.class,
                () -> converter.executeExistingPlaylistPreview(preview));

        verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");

        verifyNoMoreInteractions(destination);
        verifyNoInteractions(source, matcher);
    }

    @Test
    void shouldAvoidWritingWhenAllPlannedVideosArePresent() {
        PlaylistPreview preview = preview();

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of(
                        video("video-A"),
                        video("video-B"),
                        video("video-C")));

        ConversionResult result =
                converter.executeExistingPlaylistPreview(preview);

        assertEquals(0, result.insertedCount());
        assertEquals(3, result.skippedCount());

        verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");

        verifyNoMoreInteractions(destination);
        verifyNoInteractions(source, matcher);
    }

    @Test
    void shouldPreserveDestinationReadFailureWithoutWriting() {
        PlaylistPreview preview = preview();

        YoutubePlaylistReadException original =
                new YoutubePlaylistReadException(
                        "Falha de leitura simulada.",
                        new IOException("Falha simulada"));

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenThrow(original);

        YoutubePlaylistReadException result = assertThrows(
                YoutubePlaylistReadException.class,
                () -> converter.executeExistingPlaylistPreview(preview));

        assertSame(original, result);

        verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");

        verifyNoMoreInteractions(destination);
        verifyNoInteractions(source, matcher);
    }

    @Test
    void shouldPreserveInsertionFailureWithoutRetrying() {
        PlaylistPreview preview = preview();

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of(video("video-A")));

        YoutubeTrackInsertionException original =
                new YoutubeTrackInsertionException(
                        "Falha de inserção simulada.");

        doThrow(original).when(destination).addTracks(
                "destination-playlist",
                List.of("video-B", "video-C"));

        YoutubeTrackInsertionException result = assertThrows(
                YoutubeTrackInsertionException.class,
                () -> converter.executeExistingPlaylistPreview(preview));

        assertSame(original, result);

        verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");

        verify(destination, times(1)).addTracks(
                "destination-playlist",
                List.of("video-B", "video-C"));

        verifyNoMoreInteractions(destination);
        verifyNoInteractions(source, matcher);
    }

    @Test
    void shouldRejectNullPreviewBeforeReading() {
        assertThrows(
                NullPointerException.class,
                () -> converter.executeExistingPlaylistPreview(null));

        verifyNoInteractions(source, destination, matcher);
    }

    @Test
    void shouldRejectPreviewWithoutExistingDestination() {
        PlaylistPreview preview =
                new PlaylistPreview("source-playlist", List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> converter.executeExistingPlaylistPreview(preview));

        verifyNoInteractions(source, destination, matcher);
    }

    private PlaylistPreview preview() {
        return new PlaylistPreview(
                "source-playlist",
                "destination-playlist",
                List.of(
                        match("source-A", "video-A"),
                        match("source-B", "video-B"),
                        match("source-C", "video-C"),
                        match("source-D", null)),
                Set.of("video-A"));
    }

    private MatchResult match(String sourceId, String videoId) {
        Song sourceSong = new Song(sourceId, "Song " + sourceId,
                "Test Artist", 200000);

        return videoId == null
                ? new MatchResult(sourceSong, null, null, 0.0)
                : new MatchResult(sourceSong, videoId,
                "Matched " + videoId, 0.95);
    }

    private Song video(String videoId) {
        return new Song(videoId, "Video " + videoId,
                "Test Artist", 0);
    }

}
