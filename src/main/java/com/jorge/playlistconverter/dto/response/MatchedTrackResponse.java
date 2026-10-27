package com.jorge.playlistconverter.dto.response;

public record MatchedTrackResponse(String sourceTrackId, String sourceTitle, String sourceArtist,
                                   String destinationTrackId, String destinationTitle,
                                   double confidence, boolean alreadyPresent) {
}
