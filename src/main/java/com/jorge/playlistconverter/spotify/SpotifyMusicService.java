package com.jorge.playlistconverter.spotify;


import com.jorge.playlistconverter.model.Song;  // Era Track
import com.jorge.playlistconverter.service.MusicService;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Fase 1: autenticar (Authorization Code + PKCE, escopo playlist-read-private).
 * Fase 2: implementar getPlaylistTracks — lembrar de paginar (100 itens por página).
 * Fase 3: implementar searchCandidates usando o endpoint de Search da API.
 * Fase 6: implementar createPlaylist / addTracks (exige escopo de escrita).
 */
public class SpotifyMusicService implements MusicService {

    private final SpotifyApi readApi;
    private final SpotifyApi writeApi;
    private final SpotifyAuthService spotifyAuthService;

    public SpotifyMusicService(SpotifyApi readApi, SpotifyApi writeApi, SpotifyAuthService spotifyAuthService) {
        this.readApi = readApi;
        this.writeApi = writeApi;
        this.spotifyAuthService = spotifyAuthService;
    }

    @Override
    public List<Song> getPlaylistTracks(String playlistId) {
        try {
            return getPlaylistTracksWithApi(playlistId, readApi);

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Valid user authentication required")) {

                try {
                    spotifyAuthService.ensureAuthenticated("playlist-read-private");
                    writeApi.setAccessToken(spotifyAuthService.getSpotifyApi().getAccessToken());
                    writeApi.setRefreshToken(spotifyAuthService.getSpotifyApi().getRefreshToken());
                    return getPlaylistTracksWithApi(playlistId, writeApi);

                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            throw e;
        }
    }

    private List<Song> getPlaylistTracksWithApi(String playlistId, SpotifyApi api) {
        // TODO (Fase 2): spotifyApi.getPlaylistsItems(playlistId)... + paginação
        List<Song> songs = new ArrayList<>();
        int limit = 100;
        int offset = 0;

        try{
            while (true){
                var paging = api.getPlaylistItems(playlistId)
                        .limit(limit)
                        .offset(offset)
                        .build()
                        .execute();

                PlaylistTrack[] items = paging.getItems();

                if (items == null || items.length == 0){
                    break;
                }

                for (PlaylistTrack item: items){
                    if (item.getItem() != null){
                        IPlaylistItem playlistItem = item.getItem();

                        if (playlistItem instanceof Track){
                            Track spotifyTrack = (Track) playlistItem;

                            String artist = spotifyTrack.getArtists().length > 0
                                    ? spotifyTrack.getArtists()[0].getName()
                                    : "Unkonown";

                            songs.add(new Song(
                                    spotifyTrack.getId(),
                                    spotifyTrack.getName(),
                                    artist,
                                    spotifyTrack.getDurationMs()
                            ));
                        }
                    }
                }

                offset += limit;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar tracks da playlist: " + e.getMessage(), e);
        }

        return songs;
    }

    @Override
    public List<Song> searchCandidates(Song sourceSong) {
        // TODO (Fase 3): não se aplica a este projeto — busca de equivalência
        // acontece no YouTube. Deixe vazio ou lance UnsupportedOperationException
        // até a Fase 7 (conversão inversa), quando o Spotify também vira destino.
        throw new UnsupportedOperationException("TODO: Fase 7 (conversão inversa)");
    }

    @Override
    public String createPlaylist(String name) {
        // TODO (Fase 6)
        throw new UnsupportedOperationException("TODO: Fase 6");
    }

    @Override
    public void addTracks(String playlistId, List<String> trackIds) {
        // TODO (Fase 6)
        throw new UnsupportedOperationException("TODO: Fase 6");
    }

    @Override
    public List<Song> getPlaylistTracksAlreadyAdded(String playlistId) {
        // TODO (Fase 5): reaproveita getPlaylistTracks
        throw new UnsupportedOperationException("TODO: Fase 5");
    }
}
