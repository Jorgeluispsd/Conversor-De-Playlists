package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.enums.PlaylistVisibility;
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

        PlaylistAnalyzer analyzer = new PlaylistAnalyzer(source, destination, matcher);

        PlaylistConversionValidator validator =
                new PlaylistConversionValidator();

        PlaylistDestinationRegistry destinationRegistry =
                mock(PlaylistDestinationRegistry.class);


        NewPlaylistExecutor newPlaylistExecutor =
                new NewPlaylistExecutor(destination, validator, destinationRegistry);

        ExistingPlaylistExecutor existingPlaylistExecutor =
                new ExistingPlaylistExecutor(destination, validator, destinationRegistry);

        converter = new PlaylistConverter(analyzer, destination,
                newPlaylistExecutor, existingPlaylistExecutor, validator);
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

        when(destination.createPlaylist("Minha playlist",
                PlaylistVisibility.PRIVATE))
                .thenReturn("destination-playlist");

        ConversionResult result = executeNewPlaylistFlow();

        assertEquals("source-playlist", result.sourcePlaylistId());
        assertEquals("destination-playlist", result.destinationPlaylistId());
        assertEquals(List.of(firstMatch, missingMatch), result.matches());
        assertEquals(1, result.insertedCount());

        var order = inOrder(destination);

        order.verify(destination).searchCandidates(firstSong);
        order.verify(destination).searchCandidates(secondSong);
        order.verify(destination).createPlaylist("Minha playlist", PlaylistVisibility.PRIVATE);

        order.verify(destination).addTracks(
                "destination-playlist", List.of("video-A"));

        verifyNoMoreInteractions(destination);
    }

    @Test
    void shouldRejectInvalidDestinationNameBeforeWriting() {
        PlaylistPreview preview = new PlaylistPreview(
                "source-playlist",
                List.of(new MatchResult(
                        firstSong,
                        "video-A",
                        "Song A",
                        0.95)));

        for (String name : new String[]{null, "", "   "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> converter.executeNewPlaylistPreview(preview, name));
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
                this::executeNewPlaylistFlow);

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

        when(destination.createPlaylist("Minha playlist",
                PlaylistVisibility.PRIVATE))
                .thenReturn("destination-playlist");

        doThrow(original).when(destination).addTracks(
                "destination-playlist",
                List.of("video-A"));

        YoutubeTrackInsertionException result = assertThrows(
                YoutubeTrackInsertionException.class,
                this::executeNewPlaylistFlow);

        assertSame(original, result);

        verify(destination, times(1)).createPlaylist("Minha playlist",
                PlaylistVisibility.PRIVATE);

        verify(destination, times(1)).addTracks(
                "destination-playlist",
                List.of("video-A"));
    }

    @Test
    void shouldPreviewUnmatchedSongsWithoutWriting() {
        List<Song> firstCandidates = List.of(
                new Song("video-A", "Song A", "Artist A", 0));

        MatchResult foundMatch =
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
                .thenReturn(foundMatch);

        when(matcher.findBestMatch(secondSong, List.of()))
                .thenReturn(missingMatch);

        PlaylistPreview result =
                converter.previewNewPlaylist("source-playlist");

        assertEquals("source-playlist", result.sourcePlaylistId());
        assertEquals(2, result.totalCount());
        assertEquals(1L, result.foundCount());
        assertEquals(List.of(secondSong), result.notFoundSongs());

        assertEquals(
                List.of(foundMatch, missingMatch),
                result.matches());

        verify(source).getPlaylistTracks("source-playlist");

        verify(destination).searchCandidates(firstSong);
        verify(destination).searchCandidates(secondSong);

        verify(matcher).findBestMatch(firstSong, firstCandidates);
        verify(matcher).findBestMatch(secondSong, List.of());

        verifyNoMoreInteractions(source, destination, matcher);
    }

    private ConversionResult executeNewPlaylistFlow() {
        PlaylistPreview preview =
                converter.previewNewPlaylist("source-playlist");

        return converter.executeNewPlaylistPreview(
                preview,
                "Minha playlist"
        );
    }
}
