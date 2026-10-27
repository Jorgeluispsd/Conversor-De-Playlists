package com.jorge.playlistconverter.controller;

import com.jorge.playlistconverter.converter.preview.PlaylistPreview;
import com.jorge.playlistconverter.converter.preview.StoredPlaylistPreview;
import com.jorge.playlistconverter.dto.response.PlaylistPreviewResponse;
import com.jorge.playlistconverter.enums.PlaylistVisibility;
import com.jorge.playlistconverter.mapper.PlaylistPreviewResponseMapper;
import com.jorge.playlistconverter.service.PlaylistPreviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaylistPreviewControllerTest {

    private static final String SOURCE_ID = "1234567890123456789012";

    private static final UUID PREVIEW_ID =
            UUID.fromString("3ebf4e4c-7ebd-4c52-b5b0-d943d4647206");

    private static final Instant EXPIRES_AT =
            Instant.parse("2026-10-27T18:10:00Z");

    private PlaylistPreviewService previewService;
    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        previewService = mock(PlaylistPreviewService.class);

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        PlaylistPreviewController controller = new PlaylistPreviewController(previewService,
                new PlaylistPreviewResponseMapper());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();

        jsonMapper = JsonMapper.builder().build();
    }

    @AfterEach
    void closeValidation() {
        validator.close();
    }

    @Test
    void shouldPrepareNewPlaylistAndDefaultToPrivate() throws Exception {
        PlaylistPreview preview = new PlaylistPreview(SOURCE_ID, List.of());

        StoredPlaylistPreview stored = new StoredPlaylistPreview(PREVIEW_ID, preview,
                "Meu Rock", PlaylistVisibility.PRIVATE, EXPIRES_AT);

        when(previewService.prepareNewPlaylist(SOURCE_ID,
                "Meu Rock", PlaylistVisibility.PRIVATE)).thenReturn(stored);

        String json = """
                {
                  "sourcePlaylist": "1234567890123456789012",
                  "destinationName": "Meu Rock"
                }
                """;

        var result = mockMvc.perform(
                        post("/api/conversions/previews/new")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON)).andReturn();

        PlaylistPreviewResponse response = jsonMapper.readValue(result.getResponse().getContentAsString(),
                PlaylistPreviewResponse.class);

        assertEquals(PREVIEW_ID, response.previewId());
        assertEquals(SOURCE_ID, response.sourcePlaylistId());
        assertEquals("Meu Rock", response.destinationName());
        assertEquals(PlaylistVisibility.PRIVATE, response.visibility());
        assertEquals(EXPIRES_AT, response.expiresAt());
        assertNull(response.destinationPlaylistId());
        assertEquals(0, response.total());

        verify(previewService).prepareNewPlaylist(SOURCE_ID, "Meu Rock", PlaylistVisibility.PRIVATE);

        verifyNoMoreInteractions(previewService);
    }

    @Test
    void shouldPrepareExistingPlaylistWithoutCreationOptions()
            throws Exception {
        PlaylistPreview preview = new PlaylistPreview(SOURCE_ID,
                "PL_destination", List.of(), Set.of());

        StoredPlaylistPreview stored = new StoredPlaylistPreview(PREVIEW_ID, preview,
                null, null, EXPIRES_AT);

        when(previewService.prepareExistingPlaylist(SOURCE_ID,
                "PL_destination")).thenReturn(stored);

        String json = """
                {
                  "sourcePlaylist": "1234567890123456789012",
                  "destinationPlaylist": "PL_destination"
                }
                """;

        var result = mockMvc.perform(post("/api/conversions/previews/existing")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON)).andReturn();

        PlaylistPreviewResponse response = jsonMapper.readValue(result.getResponse().getContentAsString(),
                PlaylistPreviewResponse.class);

        assertEquals(PREVIEW_ID, response.previewId());
        assertEquals(SOURCE_ID, response.sourcePlaylistId());
        assertEquals("PL_destination", response.destinationPlaylistId());
        assertEquals(EXPIRES_AT, response.expiresAt());
        assertNull(response.destinationName());
        assertNull(response.visibility());

        verify(previewService).prepareExistingPlaylist(SOURCE_ID, "PL_destination");

        verifyNoMoreInteractions(previewService);
    }

    @Test
    void shouldRejectBlankSourceBeforePreparingNewPlaylist() throws Exception {
        String json = """
            {
              "sourcePlaylist": "   ",
              "destinationName": "Meu Rock",
              "visibility": "PRIVATE"
            }
            """;

        mockMvc.perform(post("/api/conversions/previews/new")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)).andExpect(status().isBadRequest());

        verifyNoInteractions(previewService);
    }

    @Test
    void shouldRejectBlankExistingDestinationBeforePreparingPreview()
            throws Exception {
        String json = """
            {
              "sourcePlaylist": "1234567890123456789012",
              "destinationPlaylist": "   "
            }
            """;

        mockMvc.perform(post("/api/conversions/previews/existing")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)).andExpect(status().isBadRequest());

        verifyNoInteractions(previewService);
    }

    @Test
    void shouldRejectUnknownVisibilityBeforePreparingPreview() throws Exception {
        String json = """
            {
              "sourcePlaylist": "1234567890123456789012",
              "destinationName": "Meu Rock",
              "visibility": "UNKNOWN"
            }
            """;

        mockMvc.perform(post("/api/conversions/previews/new")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)).andExpect(status().isBadRequest());

        verifyNoInteractions(previewService);
    }

    @Test
    void shouldRejectMalformedJsonBeforePreparingPreview() throws Exception {
        mockMvc.perform(post("/api/conversions/previews/new")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{")).andExpect(status().isBadRequest());

        verifyNoInteractions(previewService);
    }
}
