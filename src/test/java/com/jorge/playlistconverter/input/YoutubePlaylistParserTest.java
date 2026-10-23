package com.jorge.playlistconverter.input;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class YoutubePlaylistParserTest {

    private static final String PLAYLIST_ID = "PLexample_123-XYZ";

    private final YoutubePlaylistParser parser =
            new YoutubePlaylistParser();

    @ParameterizedTest
    @ValueSource(strings = {
            PLAYLIST_ID,
            "  " + PLAYLIST_ID + "  ",
            "https://www.youtube.com/playlist?list=" + PLAYLIST_ID,
            "https://youtube.com/playlist?list=" + PLAYLIST_ID,
            "https://music.youtube.com/playlist?list=" + PLAYLIST_ID,
            "https://www.youtube.com/watch?v=video-example&list="
                    + PLAYLIST_ID,
            "https://www.youtube.com/playlist?index=2&list="
                    + PLAYLIST_ID + "&feature=shared"
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
            "https://open.spotify.com/playlist/example",
            "https://www.youtube.com.example.org/playlist?list=PLexample",
            "http://www.youtube.com/playlist?list=PLexample",
            "https://www.youtube.com/watch?v=video-example",
            "https://www.youtube.com/playlist?list=",
            "https://www.youtube.com/channel/example?list=PLexample",
            "https://www.youtube.com/playlist?list=invalid%20id",
            "https://www.youtube.com/playlist?list=invalid id"
    })
    void shouldRejectUnsupportedOrInvalidInput(String input) {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(input)
        );
    }
}
