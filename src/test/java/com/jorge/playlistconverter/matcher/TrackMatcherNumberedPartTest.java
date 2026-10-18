package com.jorge.playlistconverter.matcher;

import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrackMatcherNumberedPartTest {

    private TrackMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new TrackMatcher();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Stephen - Crossfire Pt. II ft. Talib Kweli & KillaGraham "
                    + "(Official Music Video)",
            "Crossfire (Part 2)",
            "Crossfire Parte II",
            "Crossfire Part 3"
    })
    void shouldRejectNumberedPartWhenSourceHasNoPart(String candidateTitle) {
        Song source = new Song("source", "Crossfire",
                "Stephen", 270000);

        Song candidate = new Song("other-part", candidateTitle,
                "Stephen", 0);

        MatchResult result =
                matcher.findBestMatch(source, List.of(candidate));

        assertFalse(result.found());
    }

    @Test
    void shouldSelectOriginalInsteadOfNumberedPart() {
        Song source = new Song("source", "Crossfire",
                "Stephen", 270000);

        Song otherPart = new Song("part-two", "Stephen - Crossfire Pt. II (Official Music Video)",
                "Stephen", 0);

        Song original = new Song("original", "Stephen - Crossfire (Official Audio)",
                "Stephen", 0);

        MatchResult result = matcher.findBestMatch(
                source,
                List.of(otherPart, original));

        assertTrue(result.found());
        assertEquals("original", result.matchedId());
    }

    @Test
    void shouldAcceptEquivalentRomanAndArabicPartNumbers() {
        Song source = new Song("source", "Crossfire Pt. II",
                "Stephen", 270000);

        Song candidate = new Song("part-two", "Stephen - Crossfire Part 2 (Official Audio)",
                "Stephen", 0);

        MatchResult result =
                matcher.findBestMatch(source, List.of(candidate));

        assertTrue(result.found());
        assertEquals("part-two", result.matchedId());
    }

    @Test
    void shouldRejectCandidateWithoutSourcePartNumber() {
        Song source = new Song("source", "Crossfire Part 2",
                "Stephen", 270000);

        Song candidate = new Song("original", "Crossfire",
                "Stephen", 0);

        MatchResult result =
                matcher.findBestMatch(source, List.of(candidate));

        assertFalse(result.found());
    }

    @Test
    void shouldRejectDifferentPartNumbers() {
        Song source = new Song("source", "Crossfire Part II",
                "Stephen", 270000);

        Song candidate = new Song("part-three", "Crossfire Part III",
                "Stephen", 0);

        MatchResult result =
                matcher.findBestMatch(source, List.of(candidate));

        assertFalse(result.found());
    }
}
