package com.jorge.playlistconverter.matcher;

import com.jorge.playlistconverter.model.Song;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrackVersionMatcherTest {
    private final TrackVersionMatcher versionMatcher =
            new TrackVersionMatcher();

    @ParameterizedTest
    @CsvSource({
            "'Fulminante - Ao Vivo', 'Fulminante (Live)'",
            "'Fulminante (Ao Vivo)', 'Fulminante - Ao Vivo'",
            "'Song (Remix)', 'Artist - Song Remix'",
            "'Song (Cover)', 'Song - Cover'",
            "'Song (Instrumental)', 'Song - Instrumental'",
            "'Song', 'Artist - Song'",
            "'Song (Live Remix)', 'Song (Ao Vivo Remix)'"
    })
    void shouldAcceptMatchingVersionLabels(
            String sourceTitle,
            String candidateTitle) {
        assertTrue(versionMatcher.isCompatible(
                sourceTitle, candidateTitle));
    }

    @ParameterizedTest
    @CsvSource({
            "'Song', 'Song (Live)'",
            "'Song (Ao Vivo)', 'Song'",
            "'Song', 'Song (Remix)'",
            "'Song (Cover)', 'Song'",
            "'Song', 'Song (Instrumental)'",
            "'Song (Live Remix)', 'Song (Live)'"
    })
    void shouldRejectDifferentVersionLabels(
            String sourceTitle,
            String candidateTitle
    ) {
        assertFalse(versionMatcher.isCompatible(
                sourceTitle, candidateTitle));
    }

    @Test
    void shouldNotInterpretLiverpoolAsLiveVersion() {
        assertTrue(versionMatcher.isCompatible(
                "Liverpool", "Liverpool (Official Audio)"));
    }

    @Test
    void shouldSelectLiveAndUnlabelledTracksIndependently() {
        TrackMatcher matcher = new TrackMatcher();

        Song unlabelledSource = new Song(
                "source-original", "Fulminante", "Mumuzinho", 203906);

        Song liveSource = new Song(
                "source-live", "Fulminante - Ao Vivo",
                "Mumuzinho", 200432);

        Song unlabelledCandidate = new Song(
                "video-original", "Fulminante",
                "Mumuzinho - Topic", 0);

        Song liveCandidate = new Song(
                "video-live", "Mumuzinho - Fulminante (Ao Vivo)",
                "MumuzinhoVEVO", 0);

        List<Song> candidates =
                List.of(unlabelledCandidate, liveCandidate);

        assertEquals(
                "video-original",
                matcher.findBestMatch(
                        unlabelledSource, candidates).matchedId());

        assertEquals(
                "video-live",
                matcher.findBestMatch(
                        liveSource, candidates).matchedId());
    }
}
