package com.jorge.playlistconverter.converter;

import com.jorge.playlistconverter.model.MatchResult;

import java.util.List;

public record ConversionResult(String sourcePlaylistId,
                               String destinationPlaylistId,
                               List<MatchResult> matches, int insertedCount
) {
   public ConversionResult {
       matches = List.copyOf(matches);
   }
}
