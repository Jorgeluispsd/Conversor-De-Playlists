package com.jorge.playlistconverter.input;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SpotifyPlaylistParserTest {

    private static final String PLAYLIST_ID =
            "2S6FRrlOthv0w7ITaaB4mp";

    private final SpotifyPlaylistParser parser =
            new SpotifyPlaylistParser();

    @ParameterizedTest
    @ValueSource(strings = {
            PLAYLIST_ID,
            "  " + PLAYLIST_ID + "  ",
            "https://open.spotify.com/playlist/" + PLAYLIST_ID,
            "https://open.spotify.com/playlist/" + PLAYLIST_ID
                    + "?si=example&other=value",
            "https://open.spotify.com/intl-pt/playlist/" + PLAYLIST_ID,
            "https://open.spotify.com/playlist/" + PLAYLIST_ID + "/"
    })
    void shouldExtractSameIdFromSupportedInputs(String input) {
        String result = parser.parse(input);

        assertEquals(PLAYLIST_ID, result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectMissingInput(String input) {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(input)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://open.spotify.com/track/" + PLAYLIST_ID,
            "https://www.youtube.com/playlist?list=PLexample",
            "https://open.spotify.com.example.org/playlist/" + PLAYLIST_ID,
            "http://open.spotify.com/playlist/" + PLAYLIST_ID,
            "https://open.spotify.com/playlist/short-id",
            "https://open.spotify.com/playlist/invalid path"
    })
    void shouldRejectUnsupportedOrInvalidInput(String input) {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(input)
        );
    }
}
