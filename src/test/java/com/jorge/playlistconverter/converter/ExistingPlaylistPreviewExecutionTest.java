package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.enums.SyncRunStatus;
import com.jorge.playlistconverter.errors.conversion.PlaylistPreviewChangedException;
import com.jorge.playlistconverter.errors.persistence.SyncStateStorageException;
import com.jorge.playlistconverter.errors.youtube.YoutubePlaylistReadException;
import com.jorge.playlistconverter.errors.youtube.YoutubeTrackInsertionException;
import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;
import com.jorge.playlistconverter.state.SyncStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExistingPlaylistPreviewExecutionTest {

    private MusicService source;
    private MusicService destination;
    private TrackMatcher matcher;
    private PlaylistConverter converter;
    private PlaylistDestinationRegistry destinationRegistry;
    private SyncStateStore store;

    @BeforeEach
    void setUp() {
        source = mock(MusicService.class);
        destination = mock(MusicService.class);
        matcher = mock(TrackMatcher.class);

        store = mock(SyncStateStore.class);
        SyncRunTracker runTracker = new SyncRunTracker(store);
        TrackSyncRecorder trackSyncRecorder = new TrackSyncRecorder(store);

        PlaylistAnalyzer analyzer = new PlaylistAnalyzer(source, destination, matcher);

        PlaylistConversionValidator validator =
                new PlaylistConversionValidator();

        destinationRegistry = mock(PlaylistDestinationRegistry.class);

        when(destinationRegistry.registerDestination(
                anyString(),
                anyString())).thenReturn(10L);

        when(store.startSyncRun(anyLong())).thenReturn(20L);

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

        var order = inOrder(destination, destinationRegistry, store);

        order.verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");

        order.verify(destinationRegistry).registerDestination("source-playlist",
                "destination-playlist");

        order.verify(store).startSyncRun(10L);

        order.verify(destination).addTracks(eq("destination-playlist"),
                eq(List.of("video-B", "video-C")), any());

        order.verify(store).finishSyncRun(20L, SyncRunStatus.SUCCESS);

        verifyNoMoreInteractions(destination, destinationRegistry, store);
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

        verify(destination).addTracks(eq("destination-playlist"),
                eq(List.of("video-C")), any());

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
        verifyNoInteractions(destinationRegistry);
        verifyNoInteractions(source, matcher);
        verifyNoInteractions(store);
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

        verify(destinationRegistry).registerDestination("source-playlist",
                "destination-playlist");

        verifyNoMoreInteractions(destination, destinationRegistry);
        verifyNoInteractions(source, matcher);
        verify(store).startSyncRun(10L);
        verify(store).finishSyncRun(20L, SyncRunStatus.SUCCESS);
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
        verifyNoInteractions(destinationRegistry);
        verifyNoInteractions(source, matcher);
        verifyNoInteractions(store);
    }

    @Test
    void shouldPreserveInsertionFailureWithoutRetrying() {
        PlaylistPreview preview = preview();

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"))
                .thenReturn(List.of(video("video-A")));

        YoutubeTrackInsertionException original =
                new YoutubeTrackInsertionException(
                        "Falha de inserção simulada.");

        doThrow(original).when(destination).addTracks(eq("destination-playlist"),
                eq(List.of("video-B", "video-C")), any());

        YoutubeTrackInsertionException result = assertThrows(
                YoutubeTrackInsertionException.class,
                () -> converter.executeExistingPlaylistPreview(preview));

        assertSame(original, result);

        verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");

        verify(destinationRegistry).registerDestination("source-playlist",
                "destination-playlist");

        verify(destination, times(1)).addTracks(eq("destination-playlist"),
                eq(List.of("video-B", "video-C")), any());

        verifyNoMoreInteractions(destination, destinationRegistry);
        verifyNoInteractions(source, matcher);
        verify(store).startSyncRun(10L);
        verify(store).finishSyncRun(20L, SyncRunStatus.FAILED);
    }

    @Test
    void shouldRejectNullPreviewBeforeReading() {
        assertThrows(
                NullPointerException.class,
                () -> converter.executeExistingPlaylistPreview(null));

        verifyNoInteractions(source, destination, matcher);
        verifyNoInteractions(destinationRegistry);
        verifyNoInteractions(store);
    }

    @Test
    void shouldRejectPreviewWithoutExistingDestination() {
        PlaylistPreview preview =
                new PlaylistPreview("source-playlist", List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> converter.executeExistingPlaylistPreview(preview));

        verifyNoInteractions(source, destination, matcher);
        verifyNoInteractions(destinationRegistry);
        verifyNoInteractions(store);
    }

    @Test
    void shouldStopBeforeInsertionWhenDestinationRegistrationFails() {
        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"
        )).thenReturn(List.of(video("video-A")));

        SyncStateStorageException original =
                new SyncStateStorageException(
                        "Falha simulada no registro.",
                        new SQLException("Falha simulada"));

        when(destinationRegistry.registerDestination("source-playlist",
                "destination-playlist")).thenThrow(original);

        SyncStateStorageException result = assertThrows(
                SyncStateStorageException.class,
                () -> converter.executeExistingPlaylistPreview(preview()));

        assertSame(original, result);

        var order = inOrder(destination, destinationRegistry);

        order.verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");

        order.verify(destinationRegistry).registerDestination("source-playlist",
                "destination-playlist");

        verifyNoMoreInteractions(destination, destinationRegistry);
        verifyNoInteractions(source, matcher);
        verifyNoInteractions(store);
    }

    @Test
    void shouldNotInsertTracksWhenStartingRunFails() {
        when(destination.getPlaylistTracksAlreadyAdded(
                "destination-playlist"
        )).thenReturn(List.of(video("video-A")));

        SyncStateStorageException original =
                new SyncStateStorageException(
                        "Falha ao iniciar execução.",
                        new SQLException("Falha simulada"));

        when(store.startSyncRun(10L)).thenThrow(original);

        SyncStateStorageException result = assertThrows(
                SyncStateStorageException.class,
                () -> converter.executeExistingPlaylistPreview(preview()));

        assertSame(original, result);

        var order = inOrder(destination, destinationRegistry, store);

        order.verify(destination)
                .getPlaylistTracksAlreadyAdded("destination-playlist");

        order.verify(destinationRegistry).registerDestination("source-playlist",
                "destination-playlist");

        order.verify(store).startSyncRun(10L);

        verifyNoMoreInteractions(destination, destinationRegistry, store);
        verifyNoInteractions(source, matcher);
    }

    @Test
    void shouldRecordOnlyNewConfirmedTracksInExistingPlaylist() {
        PlaylistPreview preview = preview();

        MatchResult secondMatch = preview.matches().get(1);
        MatchResult thirdMatch = preview.matches().get(2);

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"
        )).thenReturn(List.of(video("video-A")));

        doAnswer(invocation -> {
            Consumer<String> listener = invocation.getArgument(2);

            listener.accept("video-B");
            listener.accept("video-C");

            return null;
        }).when(destination).addTracks(eq("destination-playlist"),
                eq(List.of("video-B", "video-C")), any());

        ConversionResult result =
                converter.executeExistingPlaylistPreview(preview);

        assertEquals(2, result.insertedCount());
        assertEquals(1, result.skippedCount());

        var order = inOrder(store);

        order.verify(store).startSyncRun(10L);

        order.verify(store).markSynced(10L, secondMatch.sourceSong(),
                "video-B", "success", secondMatch.confidence());

        order.verify(store).markSynced(10L, thirdMatch.sourceSong(),
                "video-C", "success", thirdMatch.confidence());

        order.verify(store).finishSyncRun(20L, SyncRunStatus.SUCCESS);

        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldKeepConfirmedTrackRecordedWhenLaterInsertionFails() {
        PlaylistPreview preview = preview();

        MatchResult secondMatch = preview.matches().get(1);

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"
        )).thenReturn(List.of(video("video-A")));

        YoutubeTrackInsertionException original =
                new YoutubeTrackInsertionException(
                        "Falha simulada na inserção de C.");

        doAnswer(invocation -> {
            Consumer<String> listener = invocation.getArgument(2);

            listener.accept("video-B");

            throw original;
        }).when(destination).addTracks(eq("destination-playlist"),
                eq(List.of("video-B", "video-C")), any());

        YoutubeTrackInsertionException result = assertThrows(
                YoutubeTrackInsertionException.class,
                () -> converter.executeExistingPlaylistPreview(preview));

        assertSame(original, result);

        var order = inOrder(store);

        order.verify(store).startSyncRun(10L);

        order.verify(store).markSynced(10L, secondMatch.sourceSong(),
                "video-B", "success", secondMatch.confidence());

        order.verify(store).finishSyncRun(20L, SyncRunStatus.FAILED);

        verifyNoMoreInteractions(store);

        verify(destination, times(1)).addTracks(eq("destination-playlist"),
                eq(List.of("video-B", "video-C")), any());
    }

    @Test
    void shouldFailExistingPlaylistRunWhenTrackRecordingFails() {
        PlaylistPreview preview = preview();

        MatchResult secondMatch = preview.matches().get(1);

        when(destination.getPlaylistTracksAlreadyAdded("destination-playlist"
        )).thenReturn(List.of(video("video-A")));

        SyncStateStorageException original =
                new SyncStateStorageException(
                        "Falha ao registrar a música confirmada.",
                        new SQLException("Falha simulada")
                );

        doThrow(original).when(store).markSynced(10L,
                secondMatch.sourceSong(), "video-B",
                "success", secondMatch.confidence());

        doAnswer(invocation -> {
            Consumer<String> listener = invocation.getArgument(2);

            listener.accept("video-B");
            listener.accept("video-C");

            return null;
        }).when(destination).addTracks(eq("destination-playlist"),
                eq(List.of("video-B", "video-C")), any());

        SyncStateStorageException result = assertThrows(
                SyncStateStorageException.class,
                () -> converter.executeExistingPlaylistPreview(preview));

        assertSame(original, result);

        var order = inOrder(store);

        order.verify(store).startSyncRun(10L);

        order.verify(store).markSynced(10L, secondMatch.sourceSong(), "video-B",
                "success", secondMatch.confidence());

        order.verify(store).finishSyncRun(20L, SyncRunStatus.FAILED);

        verifyNoMoreInteractions(store);

        verify(destination, times(1)).addTracks(eq("destination-playlist"),
                eq(List.of("video-B", "video-C")), any());
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
