package com.jorge.playlistconverter.model;

public record SyncJob(long id, String sourcePlaylistId,
                      String destinationPlaylistId, String direction) {
}
