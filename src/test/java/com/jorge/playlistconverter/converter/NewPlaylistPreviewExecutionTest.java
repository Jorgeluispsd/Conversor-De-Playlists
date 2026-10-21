package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.enums.PlaylistVisibility;
import com.jorge.playlistconverter.errors.youtube.YoutubePlaylistCreationException;
import com.jorge.playlistconverter.errors.youtube.YoutubeTrackInsertionException;
import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NewPlaylistPreviewExecutionTest {

    private MusicService source;
    private MusicService destination;
    private TrackMatcher matcher;
    private PlaylistConverter converter;

    @BeforeEach
    void setUp() {
        source = mock(MusicService.class);
        destination = mock(MusicService.class);
        matcher = mock(TrackMatcher.class);

        converter = new PlaylistConverter(source, destination, matcher);
    }

    @ParameterizedTest
    @EnumSource(PlaylistVisibility.class)
    void shouldCreatePlaylistWithSelectedVisibilityWithoutRepeatingAnalysis(
            PlaylistVisibility visibility
    ) {
        PlaylistPreview preview = preview(
                match("source-A", "video-A"),
                match("source-B", "video-B"));

        when(destination.createPlaylist("Meu Rock", visibility))
                .thenReturn("new-playlist");

        ConversionResult result = converter.executeNewPlaylistPreview(
                preview, "  Meu Rock  ", visibility);

        assertEquals("source-playlist", result.sourcePlaylistId());
        assertEquals("new-playlist", result.destinationPlaylistId());
        assertEquals(preview.matches(), result.matches());
        assertEquals(2, result.insertedCount());
        assertEquals(0, result.skippedCount());

        var order = inOrder(destination);

        order.verify(destination).createPlaylist("Meu Rock", visibility);
        order.verify(destination).addTracks(
                "new-playlist", List.of("video-A", "video-B"));

        verifyNoMoreInteractions(destination);
        verifyNoInteractions(source, matcher);
    }

    @Test
    void shouldUsePrivateVisibilityByDefault() {
        when(destination.createPlaylist(
                "Meu Rock", PlaylistVisibility.PRIVATE))
                .thenReturn("new-playlist");

        converter.executeNewPlaylistPreview(
                preview(match("source-A", "video-A")),
                "Meu Rock"
        );

        verify(destination).createPlaylist(
                "Meu Rock", PlaylistVisibility.PRIVATE);
        verify(destination).addTracks(
                "new-playlist", List.of("video-A"));

        verifyNoMoreInteractions(destination);
        verifyNoInteractions(source, matcher);
    }

    @Test
    void shouldSkipRepeatedIdsAndPreserveUnmatchedSongs() {
        MatchResult missing = new MatchResult(
                new Song("source-D", "Song D", "Artist D", 0),
                null, null, 0.0);

        PlaylistPreview preview = preview(
                match("source-A", "video-A"),
                match("source-B", "video-B"),
                match("source-C", "video-A"),
                missing);

        when(destination.createPlaylist(
                "Meu Rock", PlaylistVisibility.PRIVATE))
                .thenReturn("new-playlist");

        ConversionResult result = converter.executeNewPlaylistPreview(
                preview, "Meu Rock");

        assertEquals(2, result.insertedCount());
        assertEquals(1, result.skippedCount());
        assertEquals(preview.matches(), result.matches());
        assertEquals(missing, result.matches().get(3));

        verify(destination).addTracks(
                "new-playlist", List.of("video-A", "video-B"));

        verifyNoInteractions(source, matcher);
    }

    @Test
    void shouldRejectNullPreviewOrVisibilityBeforeWriting() {
        PlaylistPreview preview =
                preview(match("source-A", "video-A"));

        assertThrows(
                NullPointerException.class,
                () -> converter.executeNewPlaylistPreview(
                        null, "Meu Rock", PlaylistVisibility.PRIVATE));

        assertThrows(
                NullPointerException.class,
                () -> converter.executeNewPlaylistPreview(
                        preview, "Meu Rock", null));

        verifyNoInteractions(source, destination, matcher);
    }

    @Test
    void shouldRejectInvalidSourceIdOrNameBeforeWriting() {
        for (String invalid : new String[]{null, "", "   "}) {
            PlaylistPreview invalidSource = new PlaylistPreview(
                    invalid, List.of(match("source-A", "video-A")));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> converter.executeNewPlaylistPreview(
                            invalidSource, "Meu Rock"));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> converter.executeNewPlaylistPreview(
                            preview(match("source-A", "video-A")),
                            invalid));
        }

        verifyNoInteractions(source, destination, matcher);
    }

    @Test
    void shouldRejectPreviewPreparedForExistingDestination() {
        List<MatchResult> matches =
                List.of(match("source-A", "video-A"));

        PlaylistPreview withDestinationId = new PlaylistPreview(
                "source-playlist", "existing-playlist",
                matches, Set.of());

        PlaylistPreview withExistingIds = new PlaylistPreview(
                "source-playlist", null,
                matches, Set.of("existing-video"));

        for (PlaylistPreview preview :
                List.of(withDestinationId, withExistingIds)) {

            assertThrows(IllegalArgumentException.class,
                    () -> converter.executeNewPlaylistPreview(
                            preview, "Meu Rock"));
        }

        verifyNoInteractions(source, destination, matcher);
    }

    @Test
    void shouldRejectPreviewWithoutMatchedTracksBeforeCreatingPlaylist() {
        MatchResult missing = new MatchResult(
                new Song("source-A", "Song A", "Artist A", 0),
                null, null, 0.0);

        for (PlaylistPreview preview :
                List.of(preview(), preview(missing))) {

            assertThrows(IllegalArgumentException.class,
                    () -> converter.executeNewPlaylistPreview(
                            preview, "Meu Rock"));
        }

        verifyNoInteractions(source, destination, matcher);
    }

    @Test
    void shouldPreserveCreationFailureWithoutInsertingTracks() {
        YoutubePlaylistCreationException original =
                new YoutubePlaylistCreationException(
                        "Falha de criação simulada.");

        when(destination.createPlaylist(
                "Meu Rock", PlaylistVisibility.PRIVATE))
                .thenThrow(original);

        YoutubePlaylistCreationException result = assertThrows(
                YoutubePlaylistCreationException.class,
                () -> converter.executeNewPlaylistPreview(
                        preview(match("source-A", "video-A")),
                        "Meu Rock"));

        assertSame(original, result);

        verify(destination, times(1)).createPlaylist(
                "Meu Rock", PlaylistVisibility.PRIVATE);

        verifyNoMoreInteractions(destination);
        verifyNoInteractions(source, matcher);
    }

    @Test
    void shouldPreserveInsertionFailureWithoutCreatingAnotherPlaylist() {
        when(destination.createPlaylist(
                "Meu Rock", PlaylistVisibility.PRIVATE))
                .thenReturn("new-playlist");

        YoutubeTrackInsertionException original =
                new YoutubeTrackInsertionException(
                        "Falha de inserção simulada.");

        doThrow(original).when(destination).addTracks(
                "new-playlist", List.of("video-A"));

        YoutubeTrackInsertionException result = assertThrows(
                YoutubeTrackInsertionException.class,
                () -> converter.executeNewPlaylistPreview(
                        preview(match("source-A", "video-A")),
                        "Meu Rock"));

        assertSame(original, result);

        verify(destination, times(1)).createPlaylist(
                "Meu Rock", PlaylistVisibility.PRIVATE);
        verify(destination, times(1)).addTracks(
                "new-playlist", List.of("video-A"));

        verifyNoMoreInteractions(destination);
        verifyNoInteractions(source, matcher);
    }

    private PlaylistPreview preview(MatchResult... matches) {
        return new PlaylistPreview(
                "source-playlist", List.of(matches));
    }

    private MatchResult match(String sourceId, String videoId) {
        Song song = new Song(
                sourceId, "Song " + sourceId, "Artist", 0);

        return new MatchResult(
                song, videoId, "Video " + videoId, 0.90);
    }
}
