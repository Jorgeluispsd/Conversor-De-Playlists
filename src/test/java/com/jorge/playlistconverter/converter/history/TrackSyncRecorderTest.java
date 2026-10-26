package com.jorge.playlistconverter.converter.history;

import com.jorge.playlistconverter.converter.preview.PlaylistPreview;
import com.jorge.playlistconverter.errors.persistence.SyncStateStorageException;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.state.SyncStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrackSyncRecorderTest {

    private SyncStateStore store;
    private TrackSyncRecorder recorder;

    private final Song sourceSong = new Song("source-A", "Song A",
            "Artist A", 200000);

    @BeforeEach
    void setUp() {
        store = mock(SyncStateStore.class);
        recorder = new TrackSyncRecorder(store);
    }

    @Test
    void shouldRecordSourceDestinationAndConfidenceForInsertedTrack() {
        MatchResult match = new MatchResult(sourceSong, "video-A",
                "Artist A - Song A", 0.95);

        recorder.recordInsertedTrack(10L, match);

        verify(store).markSynced(10L, sourceSong, "video-A",
                "success", 0.95);

        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldRejectUnmatchedSongBeforeAccessingStorage() {
        MatchResult missing = new MatchResult(sourceSong, null,
                null, 0.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> recorder.recordInsertedTrack(10L, missing));

        verifyNoInteractions(store);
    }

    @Test
    void shouldPreserveStorageFailureWithoutRetrying() {
        MatchResult match = new MatchResult(sourceSong, "video-A",
                "Song A", 0.95);

        SyncStateStorageException original =
                new SyncStateStorageException("Falha ao registrar a faixa.",
                        new SQLException("Falha simulada"));

        doThrow(original).when(store).markSynced(10L,
                sourceSong, "video-A",
                "success", 0.95);

        SyncStateStorageException result = assertThrows(
                SyncStateStorageException.class,
                () -> recorder.recordInsertedTrack(10L, match));

        assertSame(original, result);

        verify(store, times(1)).markSynced(
                10L,
                sourceSong,
                "video-A",
                "success",
                0.95);

        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldRecordOnlyTheConfirmedVideoFromPreview() {
        Song secondSong = new Song(
                "source-B", "Song B", "Artist B", 180000
        );

        MatchResult firstMatch = new MatchResult(
                sourceSong, "video-A", "Song A", 0.95);

        MatchResult secondMatch = new MatchResult(
                secondSong, "video-B", "Song B", 0.90);

        MatchResult missing = new MatchResult(
                new Song("source-C", "Song C", "Artist C", 160000),
                null, null, 0.0);

        PlaylistPreview preview = new PlaylistPreview(
                "source-playlist",
                List.of(firstMatch, secondMatch, missing));

        Consumer<String> listener = recorder.insertedListener(10L, preview);

        verifyNoInteractions(store);

        listener.accept("video-A");

        verify(store).markSynced(
                10L, sourceSong, "video-A", "success", 0.95);

        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldRecordEverySourceMatchForTheConfirmedVideo() {
        Song secondSong = new Song(
                "source-B", "Song B", "Artist B", 180000);

        PlaylistPreview preview = new PlaylistPreview(
                "source-playlist",
                List.of(
                        new MatchResult(
                                sourceSong, "video-A", "Video A", 0.95),
                        new MatchResult(
                                secondSong, "video-A", "Video A", 0.90)));

        Consumer<String> listener = recorder.insertedListener(10L, preview);

        listener.accept("video-A");

        verify(store).markSynced(
                10L, sourceSong, "video-A", "success", 0.95);

        verify(store).markSynced(
                10L, secondSong, "video-A", "success", 0.90);

        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldPreserveStorageFailureFromListenerWithoutRetrying() {
        PlaylistPreview preview = new PlaylistPreview(
                "source-playlist",
                List.of(new MatchResult(
                        sourceSong, "video-A", "Song A", 0.95)));

        SyncStateStorageException original = new SyncStateStorageException(
                "Falha ao registrar a faixa.",
                new SQLException("Falha simulada"));

        doThrow(original).when(store).markSynced(
                10L, sourceSong, "video-A", "success", 0.95);

        Consumer<String> listener = recorder.insertedListener(10L, preview);

        SyncStateStorageException result = assertThrows(
                SyncStateStorageException.class,
                () -> listener.accept("video-A"));

        assertSame(original, result);

        verify(store, times(1)).markSynced(
                10L, sourceSong, "video-A", "success", 0.95);

        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldRejectNullPreviewBeforeCreatingListener() {
        assertThrows(
                NullPointerException.class,
                () -> recorder.insertedListener(10L, null));

        verifyNoInteractions(store);
    }
}
