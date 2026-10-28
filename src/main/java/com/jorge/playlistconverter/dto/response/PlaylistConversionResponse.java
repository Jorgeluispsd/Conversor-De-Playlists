package com.jorge.playlistconverter.dto.response;

import java.util.List;

public record PlaylistConversionResponse(String sourcePlaylistId, String destinationPlaylistId,
                                         String destinationPlaylistUrl, int insertedCount, int skippedCount,
                                         int notFoundCount, List<UnmatchedTrackResponse> notFound) {
    public PlaylistConversionResponse{
        notFound = List.copyOf(notFound);
    }
}
