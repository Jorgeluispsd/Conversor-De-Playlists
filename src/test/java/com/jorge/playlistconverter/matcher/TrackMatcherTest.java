package com.jorge.playlistconverter.matcher;

import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class TrackMatcherTest {

    @Test
    void shouldMatchWhenTitleAndArtistAreEqual() {

        // Arrange
        Song source = new Song(
                "spotify-id",
                "Numb",
                "Linkin Park",
                185_000
        );

        Song candidate = new Song(
                "youtube-id",
                "Numb",
                "Linkin Park",
                185_000
        );

        TrackMatcher matcher = new TrackMatcher();

        // Act
        MatchResult result = matcher.findBestMatch(
                source,
                List.of(candidate)
        );

        // Assert
        assertTrue(result.found());
        assertEquals("youtube-id", result.matchedId());
        assertEquals("Numb", result.matchedTitle());
    }

    @Test
    void shouldChooseBestMatchAmongMultipleCandidates(){

        Song source = new Song("spotify-id",
                "Numb", "Linkin Park", 185_000);

        Song correctCandidate = new Song("youtube-numb",
                "Numb", "Linkin Park", 185_000);

        Song sameArtistWrongSong = new Song("youtube-in-the-end",
                "In The End", "Linkin Park", 216_000);

        Song completelyDifferent = new Song("youtube-chop-suey",
                "Chop Suey!", "System of A Down", 210_000);


        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(sameArtistWrongSong, completelyDifferent, correctCandidate));

        assertTrue(result.found());
        assertEquals("youtube-numb", result.matchedId());
        assertEquals("Numb", result.matchedTitle());
    }

    @Test
    void shouldMatchWhenYoutubeTitleContainsExtraInformation(){

        Song source = new Song("spotify-id", "Numb",
                "Linkin Park", 185_000);

        Song candidate = new Song("youtube-id", "Linkin Park - Numb (Official Music Video)",
                "Linkin Park", 185_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(candidate));

        assertTrue(result.found());
        assertEquals("youtube-id", result.matchedId());
    }

    @Test
    void shouldNotPreferLiveVersionWhenSourceIsStudioVersion(){

        Song source = new Song("spotify-id", "Numb",
                "Linkin Park", 185_000);

        Song liveCandidate = new Song("youtube-live", "Numb (Live)",
                "Linkin Park", 190_000);

        Song studioCandidate = new Song("youtube-studio", "Numb",
                "Linkin Park", 185_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(liveCandidate, studioCandidate));

        assertTrue(result.found());
        assertEquals("youtube-studio", result.matchedId());
    }

    @Test
    void shouldPenalizeLiveVersionSourceIsStudioVersion(){

        Song source = new Song("spotify-id", "Numb",
                "Linkin Park", 185_000);

        Song liveCandidate = new Song("youtube-live", "Numb (Live)",
                "Linkin Park", 190_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(liveCandidate));


        assertTrue(result.found());
        assertEquals("youtube-live", result.matchedId());
        assertEquals(0.5, result.confidence(), 0.001);
    }

    @Test
    void shouldMatchLiveVersionWhenSourceIsAlsoLive(){

        Song source = new Song("spotify-id", "Numb (Live)",
                "Linkin Park", 190_000);

        Song liveCandidate = new Song("youtube-live", "Numb (Live)",
                "Linkin Park", 190_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(liveCandidate));

        System.out.println("Live -> Live confidence: " + result.confidence());

        assertTrue(result.found());
        assertEquals("youtube-live", result.matchedId());
    }
}
