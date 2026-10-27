package com.jorge.playlistconverter.dto.request;

import com.jorge.playlistconverter.enums.PlaylistVisibility;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

public record NewPlaylistPreviewRequest(@NotBlank(message = "Informe o link ou o ID da playlist de origem.")
                                        String sourcePlaylist,

                                        @NotBlank(message = "Informe o nome da playlist de destino.")
                                        String destinationName,

                                        PlaylistVisibility visibility) {
    public NewPlaylistPreviewRequest{
        visibility = Objects.requireNonNullElse(visibility, PlaylistVisibility.PRIVATE);
    }
}
