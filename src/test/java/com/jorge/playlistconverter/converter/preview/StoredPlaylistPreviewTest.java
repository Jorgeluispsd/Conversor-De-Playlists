package com.jorge.playlistconverter.converter.preview;

import com.jorge.playlistconverter.enums.PlaylistVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;class StoredPlaylistPreviewTest {

    private static final Instant EXPIRES_AT =
            Instant.parse("2026-10-26T18:10:00Z");

    private StoredPlaylistPreview storedPreview;

    @BeforeEach
    void setUp() {
        PlaylistPreview preview = new PlaylistPreview(
                "source-playlist", List.of());

        storedPreview = new StoredPlaylistPreview(
                UUID.randomUUID(), preview, "Minha playlist",
                PlaylistVisibility.PRIVATE, EXPIRES_AT);
    }

    @Test
    void shouldRemainValidBeforeExpiration() {
        Instant beforeExpiration = EXPIRES_AT.minusSeconds(1);

        assertFalse(storedPreview.isExpired(beforeExpiration));
    }

    @Test
    void shouldExpireAtExactExpirationTime() {
        assertTrue(storedPreview.isExpired(EXPIRES_AT));
    }

    @Test
    void shouldRemainExpiredAfterExpirationTime() {
        Instant afterExpiration = EXPIRES_AT.plusSeconds(1);

        assertTrue(storedPreview.isExpired(afterExpiration));
    }

    @Test
    void shouldRejectNullCurrentTime() {
        assertThrows(NullPointerException.class,
                () -> storedPreview.isExpired(null));
    }
}
