package com.jorge.playlistconverter.spotify;

import com.jorge.playlistconverter.model.Track;
import com.jorge.playlistconverter.service.MusicService;
import se.michaelthelin.spotify.SpotifyApi;

import java.util.List;

/**
 * Fase 1: autenticar (Authorization Code + PKCE, escopo playlist-read-private).
 * Fase 2: implementar getPlaylistTracks — lembrar de paginar (100 itens por página).
 * Fase 3: implementar searchCandidates usando o endpoint de Search da API.
 * Fase 6: implementar createPlaylist / addTracks (exige escopo de escrita).
 */
public class SpotifyMusicService implements MusicService {

    private final SpotifyApi spotifyApi;

    public SpotifyMusicService(SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
    }

    @Override
    public List<Track> getPlaylistTracks(String playlistId) {
        // TODO (Fase 2): spotifyApi.getPlaylistsItems(playlistId)... + paginação
        throw new UnsupportedOperationException("TODO: Fase 2");
    }

    @Override
    public List<Track> searchCandidates(Track sourceTrack) {
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
    public List<Track> getPlaylistTracksAlreadyAdded(String playlistId) {
        // TODO (Fase 5): reaproveita getPlaylistTracks
        throw new UnsupportedOperationException("TODO: Fase 5");
    }
}
