package com.jorge.playlistconverter.dto.request;

import com.jorge.playlistconverter.enums.PlaylistVisibility;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class PlaylistPreviewRequestTest {

    private static final String SOURCE_ID = "1234567890123456789012";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidation() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidation() {
        factory.close();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void shouldRejectMissingSourceForNewPlaylist(String source) {
        var request = new NewPlaylistPreviewRequest(
                source, "Meu Rock", PlaylistVisibility.PRIVATE);

        assertOnlyInvalidField(request, "sourcePlaylist");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void shouldRejectMissingDestinationName(String name) {
        var request = new NewPlaylistPreviewRequest(
                SOURCE_ID, name, PlaylistVisibility.PRIVATE);

        assertOnlyInvalidField(request, "destinationName");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void shouldRejectMissingSourceForExistingPlaylist(String source) {
        var request = new ExistingPlaylistPreviewRequest(
                source, "PL_destination");

        assertOnlyInvalidField(request, "sourcePlaylist");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void shouldRejectMissingExistingDestination(String destination) {
        var request = new ExistingPlaylistPreviewRequest(
                SOURCE_ID, destination);

        assertOnlyInvalidField(request, "destinationPlaylist");
    }

    @ParameterizedTest
    @EnumSource(PlaylistVisibility.class)
    void shouldAcceptEachSupportedVisibility(PlaylistVisibility visibility) {
        var request = new NewPlaylistPreviewRequest(
                SOURCE_ID, "Meu Rock", visibility);

        assertTrue(validator.validate(request).isEmpty());
        assertEquals(visibility, request.visibility());
    }

    @Test
    void shouldAcceptFilledExistingPlaylistRequest() {
        var request = new ExistingPlaylistPreviewRequest(
                SOURCE_ID, "PL_destination");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldDefaultToPrivateWhenJsonOmitsVisibility() throws Exception {
        String json = """
                {
                  "sourcePlaylist": "1234567890123456789012",
                  "destinationName": "Meu Rock"
                }
                """;

        NewPlaylistPreviewRequest request = JsonMapper.builder()
                .build()
                .readValue(json, NewPlaylistPreviewRequest.class);

        assertEquals(SOURCE_ID, request.sourcePlaylist());
        assertEquals("Meu Rock", request.destinationName());
        assertEquals(PlaylistVisibility.PRIVATE, request.visibility());

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectUnknownVisibilityInJson() {
        String json = """
                {
                  "sourcePlaylist": "1234567890123456789012",
                  "destinationName": "Meu Rock",
                  "visibility": "UNKNOWN"
                }
                """;

        JsonMapper mapper = JsonMapper.builder().build();

        assertThrows(JacksonException.class,
                () -> mapper.readValue(json, NewPlaylistPreviewRequest.class));
    }

    private void assertOnlyInvalidField(Object request, String field) {
        Set<String> invalidFields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of(field), invalidFields);
    }
}
