package com.jorge.playlistconverter.converter.preview;

import com.jorge.playlistconverter.enums.PlaylistVisibility;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StoredPlaylistPreview(UUID id, PlaylistPreview preview,
                                    String destinationName, PlaylistVisibility visibility,
                                    Instant expiresAt) {

    public StoredPlaylistPreview {
        Objects.requireNonNull(id, "O identificador da prévia deve ser informado.");
        Objects.requireNonNull(preview, "A prévia deve ser informada.");
        Objects.requireNonNull(expiresAt,"O horário de expiração deve ser informado.");
    }

    public boolean isExpired(Instant now){
        Objects.requireNonNull(now, "O horário atual deve ser informado.");
        return !now.isBefore(expiresAt);
    }
}
