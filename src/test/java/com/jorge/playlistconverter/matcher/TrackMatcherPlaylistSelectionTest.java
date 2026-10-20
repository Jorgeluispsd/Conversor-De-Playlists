package com.jorge.playlistconverter.matcher;

import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
class TrackMatcherPlaylistSelectionTest {

    private TrackMatcher matcher;

    private final Song atiradores = new Song(
            "source-A",
            "Atiradores Funk Mix",
            "LPT Zlatan",
            146729);

    private final Song vitoria = new Song(
            "source-B",
            "Vitória Regia",
            "Franco, The Sir!",
            195582);

    private final Song individualTrack = new Song(
            "OUM0zmYIthA",
            "4. LPT ZLATAN - Atiradores Funk (MDMR LADO B - DJ SET)",
            "LPT Zlatan",
            0);

    private final Song fullSet = new Song(
            "lz72zgi2DM0",
            "LPT Zlatan - Meu Dinheiro, Minhas Regras "
                    + "- LADO B (DJ SET COMPLETO)",
            "LPT Zlatan",
            0);

    private final Song correctVitoria = new Song(
            "WAoCJ1OXGPg",
            "&quot;VITÓRIA REGIA&quot; ft. Franco The Sir, "
                    + "M&#39;Dep, Ian Trunks, Yuri Redicopa",
            "IAN TRUNKS",
            0);

    private final Song otherSong = new Song(
            "CAU1iAJUUdM",
            "FUJIFILM - Franco, The Sir!",
            "Franco, The Sir!",
            0);

    private final Song reaction = new Song(
            "reaction-video",
            "REACT &quot;Vitória Régia - Franco The Sir, "
                    + "M&#39;Dep, Ian Trunks, Yuri Redicopa&quot;",
            "Preitor",
            0);

    @BeforeEach
    void setUp() {
        matcher = new TrackMatcher();
    }

    @Test
    void shouldChooseIndividualTrackInsteadOfFullSet() {
        Song anotherTrack = new Song(
                "another-track",
                "2. LPT ZLATAN - Quem Diria Nois Funk "
                        + "(MDMR LADO B - DJ SET)",
                "LPT Zlatan",
                0);

        MatchResult result = matcher.findBestMatch(
                atiradores,
                List.of(fullSet, anotherTrack, individualTrack));

        assertTrue(result.found());
        assertEquals("OUM0zmYIthA", result.matchedId());
    }

    @Test
    void shouldChooseMatchingTitleDespiteDifferentChannelName() {
        MatchResult result = matcher.findBestMatch(
                vitoria,
                List.of(otherSong, reaction, correctVitoria));

        assertTrue(result.found());
        assertEquals("WAoCJ1OXGPg", result.matchedId());
    }

    @Test
    void shouldRejectFullSetWhenNoIndividualTrackIsAvailable() {
        MatchResult result =
                matcher.findBestMatch(atiradores, List.of(fullSet));

        assertFalse(result.found());
    }

    @Test
    void shouldRejectDifferentTitleEvenWhenArtistMatches() {
        MatchResult result =
                matcher.findBestMatch(vitoria, List.of(otherSong));

        assertFalse(result.found());
    }

    @Test
    void shouldRejectReactionWhenSourceIsOriginalSong() {
        MatchResult result =
                matcher.findBestMatch(vitoria, List.of(reaction));

        assertFalse(result.found());
    }
}
