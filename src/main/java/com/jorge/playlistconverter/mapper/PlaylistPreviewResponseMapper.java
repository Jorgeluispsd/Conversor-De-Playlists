package com.jorge.playlistconverter.mapper;

import com.jorge.playlistconverter.converter.preview.PlaylistPreview;
import com.jorge.playlistconverter.converter.preview.StoredPlaylistPreview;
import com.jorge.playlistconverter.dto.response.MatchedTrackResponse;
import com.jorge.playlistconverter.dto.response.PlaylistPreviewResponse;
import com.jorge.playlistconverter.dto.response.UnmatchedTrackResponse;
import com.jorge.playlistconverter.model.MatchResult;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PlaylistPreviewResponseMapper {

    public PlaylistPreviewResponse toResponse(StoredPlaylistPreview stored){
        Objects.requireNonNull(stored, "A prévia armazenada deve ser informada.");

        PlaylistPreview preview = stored.preview();

        List<MatchedTrackResponse> matches = preview.matches().stream()
                .filter(MatchResult::found)
                .map(match -> toMatchedTrackResponse(match, preview.existingDestinationIds())).toList();

        List<UnmatchedTrackResponse> notFound = preview.notFoundSongs().stream()
                .map(song -> new UnmatchedTrackResponse(song.id(), song.title(), song.artist())).toList();

        return new PlaylistPreviewResponse(stored.id(), preview.sourcePlaylistId(), preview.destinationPlaylistId(),
                stored.destinationName(), stored.visibility(), stored.expiresAt(), preview.totalCount(),
                preview.foundCount(), preview.alreadyPresentCount(), preview.newTrackCount(),
                preview.repeatedCandidateCount(), notFound.size(), matches, notFound);
    }

    private MatchedTrackResponse toMatchedTrackResponse(MatchResult match, Set<String> existingDestinationIds){
        return new MatchedTrackResponse(match.sourceSong().id(), match.sourceSong().title(), match.sourceSong().artist(),
                match.matchedId(), match.matchedTitle(), match.confidence(),
                existingDestinationIds.contains(match.matchedId()));
    }
}
