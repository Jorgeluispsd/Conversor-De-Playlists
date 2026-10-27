package com.jorge.playlistconverter.matcher;

import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrackMatcherMinimumConfidenceTest {


    private TrackMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new TrackMatcher();
    }

    @Test
    void shouldRejectCandidateBelowMinimumConfidence() {
        Song source = new Song("source-A", "Minha Musica",
                "AAAA", 180000);

        Song candidate = new Song("video-A", "Minha Musica Complemento",
                "ZZZZ", 180000);

        MatchResult result = matcher.findBestMatch(source, List.of(candidate));

        assertFalse(result.found());
        assertNull(result.matchedId());
    }

    @Test
    void shouldAcceptCandidateAtExactMinimumConfidence() {
        Song source = new Song("source-A", "Minha Musica",
                "AAAA", 180000);

        Song candidate = new Song("video-A", "Minha Musica",
                "ZZZZ", 180000);

        MatchResult result = matcher.findBestMatch(source,
                List.of(candidate));

        assertTrue(result.found());
        assertEquals("video-A", result.matchedId());
        assertEquals(0.6, result.confidence(), 0.000001);
    }

    @Test
    void shouldKeepStrongMatchAboveMinimumConfidence() {
        Song source = new Song("source-A", "Aventura Noturna",
                "Perera DJ", 180000);

        Song candidate = new Song("video-A", "Aventura Noturna",
                "Perera DJ", 180000);

        MatchResult result = matcher.findBestMatch(source,
                List.of(candidate));

        assertTrue(result.found());
        assertEquals("video-A", result.matchedId());
        assertEquals(1.0, result.confidence(), 0.000001);
    }
}
