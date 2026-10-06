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

    @Test
    void shouldPenalizeRemixVersionWhenSourceIsOriginalVersion(){

        Song source = new Song("spotify-id", "Numb",
                "Linkin Park", 185_000);

        Song remixCandidate = new Song("youtube-remix", "Numb (Remix)",
                "Linkin Park", 185_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(remixCandidate));

        System.out.println(
                "Original -> Remix confidence: " + result.confidence()
        );

        assertTrue(result.found());
        assertEquals("youtube-remix", result.matchedId());
    }

    @Test
    void shouldMatchRemixVersionWhenSourceIsAlsoRemix(){

        Song source = new Song("spotify-id", "Numb (Remix)",
                "Linkin Park", 185_000);

        Song remixCandidate = new Song("youtube-remix", "Numb (Remix)",
                "Linkin Park", 185_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(remixCandidate));

        assertTrue(result.found());
        assertEquals("youtube-remix", result.matchedId());
        assertEquals(1.0, result.confidence(), 0.001);
    }

    @Test
    void shouldPenalizeCoverVersionWhenSourceIsOriginalVersion() {

        Song source = new Song("spotify-id", "Numb",
                "Linkin Park", 185_000);

        Song coverCandidate = new Song("youtube-cover", "Numb (Cover)",
                "Linkin Park", 185_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(coverCandidate));

        assertTrue(result.found());
        assertEquals("youtube-cover", result.matchedId());
        assertEquals(0.5, result.confidence(), 0.001);
    }

    @Test
    void shouldMatchCoverVersionWhenSourceIsAlsoCover() {

        Song source = new Song("spotify-id", "Numb (Cover)",
                "Linkin Park", 185_000);

        Song coverCandidate = new Song("youtube-cover", "Numb (Cover)",
                "Linkin Park", 185_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(coverCandidate));

        assertTrue(result.found());
        assertEquals("youtube-cover", result.matchedId());
        assertEquals(1.0, result.confidence(), 0.001);
    }

    @Test
    void shouldPenalizeInstrumentalVersionWhenSourceIsOriginalVersion() {

        Song source = new Song("spotify-id", "Numb",
                "Linkin Park", 185_000);

        Song instrumentalCandidate = new Song("youtube-instrumental", "Numb (Instrumental)",
                "Linkin Park", 185_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(instrumentalCandidate));

        assertTrue(result.found());
        assertEquals("youtube-instrumental", result.matchedId());
        assertEquals(0.5, result.confidence(), 0.001);
    }

    @Test
    void shouldMatchInstrumentalVersionWhenSourceIsAlsoInstrumental() {

        Song source = new Song("spotify-id", "Numb (Instrumental)",
                "Linkin Park", 185_000);

        Song instrumentalCandidate = new Song("youtube-instrumental", "Numb (Instrumental)",
                "Linkin Park", 185_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(instrumentalCandidate));

        assertTrue(result.found());
        assertEquals("youtube-instrumental", result.matchedId());
        assertEquals(1.0, result.confidence(), 0.001);
    }

    @Test
    void shouldEvaluateCandidateWithSameTitleButDifferentArtist() {

        Song source = new Song("spotify-id", "Numb",
                "Linkin Park", 185_000);

        Song candidate = new Song("youtube-id", "Numb",
                "Different Artist", 185_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(candidate));

        System.out.println("Same title / different artist confidence: " + result.confidence());

        assertEquals("youtube-id", result.matchedId());
    }

    @Test
    void shouldPreferCandidateWithMatchingArtistWhenTitlesAreEqual() {

        Song source = new Song("spotify-id", "Numb",
                "Linkin Park", 185_000);

        Song differentArtist = new Song("youtube-other", "Numb",
                "Different Artist", 185_000);

        Song correctArtist = new Song("youtube-official", "Numb",
                "Linkin Park", 185_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(differentArtist, correctArtist));

        assertTrue(result.found());
        assertEquals("youtube-official", result.matchedId());
    }

    @Test
    void shouldPreferOfficialArtistCandidateOverProducerAndShorts() {

        Song source = new Song("spotify-id", "My Song",
                "Small Artist", 180_000);

        Song shortsCandidate = new Song("youtube-shorts", "My Song #shorts #music #viral",
                "Random Channel", 60_000);

        Song producerCandidate = new Song("youtube-producer", "Small Artist - My Song",
                "Producer Records", 180_000);

        Song artistCandidate = new Song("youtube-artist", "My Song",
                "Small Artist", 180_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source,
                List.of(shortsCandidate, producerCandidate, artistCandidate));

        System.out.println("Selected: " + result.matchedTitle());
        System.out.println("Confidence: " + result.confidence());

        assertTrue(result.found());
        assertEquals("youtube-artist", result.matchedId());
    }

    @Test
    void shouldPreferProducerCandidateOverShorts() {

        Song source = new Song("spotify-id", "Surto",
                "Realygust", 200_000);

        Song shortsCandidate = new Song("youtube-shorts", "Surto clipe oficial #shorts #fyp #trap #drake",
                "Random Channel", 60_000);

        Song producerCandidate = new Song("youtube-producer", "Realygust - Surto (Official Lyric Visualizer)",
                "Realygust", 200_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(shortsCandidate, producerCandidate));

        System.out.println("Selected: " + result.matchedTitle());
        System.out.println("Confidence: " + result.confidence());

        assertTrue(result.found());
        assertEquals("youtube-producer", result.matchedId());
    }

    @Test
    void shouldNotFindMatchWhenCandidateListIsEmpty() {

        Song source = new Song("spotify-id", "Numb",
                "Linkin Park", 185_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of());

        assertFalse(result.found());
    }

    @Test
    void shouldNotFindMatchWhenCandidatesAreCompletelyDifferent() {

        Song source = new Song("spotify-id", "Numb",
                "Linkin Park", 185_000);

        Song candidate1 = new Song("youtube-1", "Chop Suey!",
                "System Of A Down", 210_000);

        Song candidate2 = new Song("youtube-2", "Smells Like Teen Spirit",
                "Nirvana", 300_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source, List.of(candidate1, candidate2));

        System.out.println("No valid match confidence: " + result.confidence());

        assertFalse(result.found());
    }

    @Test
    void shouldChooseReasonableCandidateWhenPerfectMatchDoesNotExist() {

        Song source = new Song("spotify-id", "Surto",
                "Realygust", 200_000);

        Song reasonableCandidate = new Song("youtube-reasonable", "GUST - Surto (Official Lyric Visualizer)",
                "REALYGUST", 200_000);

        Song wrongCandidate1 = new Song("youtube-wrong-1", "O Surto - Clipe Tudo é possivel",
                "mateusgomt", 200_000);

        Song wrongCandidate2 = new Song("youtube-wrong-2", "GUST - MAU PRESSÁGIO (CLIPE OFICIAL)",
                "REALYGUST", 200_000);

        TrackMatcher matcher = new TrackMatcher();

        MatchResult result = matcher.findBestMatch(source,
                List.of(wrongCandidate1, wrongCandidate2, reasonableCandidate));

        System.out.println("Selected: " + result.matchedTitle());
        System.out.println("Confidence: " + result.confidence());

        assertTrue(result.found());
        assertEquals("youtube-reasonable", result.matchedId());
    }
}
