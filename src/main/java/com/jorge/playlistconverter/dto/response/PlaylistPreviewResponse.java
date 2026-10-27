package com.jorge.playlistconverter.dto.response;

import com.jorge.playlistconverter.enums.PlaylistVisibility;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistPreviewResponse (UUID previewId, String sourcePlaylistId, String destinationPlaylistId,
                                       String destinationName, PlaylistVisibility visibility, Instant expiresAt,
                                       int total, long found, long alreadyPresent, int newTrackCount,
                                       long repeatedCandidateCount, int notFoundCount,
                                       List<MatchedTrackResponse> matches, List<UnmatchedTrackResponse> notFound){
    public PlaylistPreviewResponse{
        matches = List.copyOf(matches);
        notFound = List.copyOf(notFound);
    }
}
