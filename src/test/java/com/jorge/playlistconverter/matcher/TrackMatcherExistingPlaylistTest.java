package com.jorge.playlistconverter.matcher;

import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrackMatcherExistingPlaylistTest {

    private TrackMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new TrackMatcher();
    }

    @Test
    void shouldReuseExistingOfficialAudio() {
        MatchResult result = compare("Crossfire", "Stephen",
                "Stephen - Crossfire (Official Audio)", "Stephen");

        assertTrue(result.found());
        assertEquals("existing-video", result.matchedId());
    }

    @Test
    void shouldRejectDifferentSongFromSameArtist() {
        MatchResult result = compare("Crossfire", "Stephen",
                "Another Song", "Stephen");

        assertFalse(result.found());
    }

    @Test
    void shouldRejectSameTitleFromDifferentArtist() {
        MatchResult result = compare("Crossfire", "Stephen",
                "Crossfire", "Another Artist");

        assertFalse(result.found());
    }

    @Test
    void shouldNotTreatPartTwoAsOriginal() {
        MatchResult result = compare("Crossfire", "Stephen",
                "Stephen - Crossfire Pt. II (Official Audio)", "Stephen");

        assertFalse(result.found());
    }

    @Test
    void shouldNotTreatLiveVersionAsStudioVersion() {
        MatchResult result = compare("Crossfire", "Stephen",
                "Stephen - Crossfire (Live)", "Stephen");

        assertFalse(result.found());
    }

    @Test
    void shouldPreserveRemasterQualifier() {
        MatchResult result = compare("Crossfire", "Stephen",
                "Stephen - Crossfire (2015 Remaster)", "Stephen");

        assertFalse(result.found());
    }

    @Test
    void shouldReturnNoMatchForEmptyDestination() {
        Song source = new Song("source", "Crossfire",
                "Stephen", 270000);

        MatchResult result = matcher.findExistingMatch(source, List.of());

        assertFalse(result.found());
    }

    private MatchResult compare(String sourceTitle, String sourceArtist,
            String existingTitle, String existingArtist
    ) {
        Song source = new Song("source", sourceTitle,
                sourceArtist, 270000);

        Song existing = new Song("existing-video", existingTitle,
                existingArtist, 0);

        return matcher.findExistingMatch(source, List.of(existing));
    }
}
