package com.jorge.playlistconverter.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ExistingPlaylistPreviewRequest(@NotBlank(message = "Informe o link ou o ID da playlist de origem.")
                                             String sourcePlaylist,
                                             @NotBlank(message = "Informe o link ou o ID da playlist de destino.")
                                             String destinationPlaylist) {
}
