package com.jorge.playlistconverter.spotify;


import com.jorge.playlistconverter.errors.OperationInterruptedException;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.exceptions.detailed.ForbiddenException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.specification.Paging;

import java.util.ArrayList;
import java.util.List;

/**
 * Fase 1: autenticar (Authorization Code + PKCE, escopo playlist-read-private).
 * Fase 2: implementar getPlaylistTracks — lembrar de paginar (100 itens por página).
 * Fase 3: implementar searchCandidates usando o endpoint de Search da API.
 * Fase 6: implementar createPlaylist / addTracks (exige escopo de escrita).
 */
public class SpotifyMusicService implements MusicService {

    private static final String READ_SCOPES = "playlist-read-private playlist-read-collaborative";

    private final SpotifyAuthService spotifyAuthService;

    public SpotifyMusicService(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
    }

    @Override
    public List<Song> getPlaylistTracks(String playlistId) {
        try {
            spotifyAuthService.ensureAuthenticated(READ_SCOPES);

        }catch (InterruptedException e){
            Thread.currentThread().interrupt();

            throw new OperationInterruptedException(
                    "Leitura da playlist interrompida durante a autenticação Spotify.", e);

        }catch (RuntimeException e){
            throw e;

        } catch (Exception e) {
            throw new RuntimeException("Falha ao autenticar no Spotify", e);
        }
        return getPlaylistTracksWithApi(playlistId, spotifyAuthService.getSpotifyApi());
    }

    private Paging<PlaylistTrack> requestPlaylistPage(SpotifyApi api, String playlistId,
                                                      int limit, int offset) throws Exception{

        return api.getPlaylistItems(playlistId).
                limit(limit)
                .offset(offset)
                .build()
                .execute();
    }

    private Paging<PlaylistTrack> readPlaylistPage(SpotifyApi api, String playlistId,
                                                   int limit, int offset) throws Exception{

        spotifyAuthService.ensureAuthenticated(READ_SCOPES);

        try {
            return requestPlaylistPage(api, playlistId, limit, offset);

        }catch (UnauthorizedException e){
            spotifyAuthService.invalidateAccessToken();
            spotifyAuthService.ensureAuthenticated(READ_SCOPES);
        }

        return requestPlaylistPage(api, playlistId, limit, offset);
    }

    private List<Song> getPlaylistTracksWithApi(String playlistId, SpotifyApi api) {
        // TODO (Fase 2): spotifyApi.getPlaylistsItems(playlistId)... + paginação
        List<Song> songs = new ArrayList<>();
        int limit = 50;
        int offset = 0;

        try{
            while (true){
                var paging = readPlaylistPage(api, playlistId, limit, offset);

                PlaylistTrack[] items = paging.getItems();

                if (items == null || items.length == 0){
                    break;
                }

                for (PlaylistTrack item: items){
                    if (item.getItem() != null){
                        IPlaylistItem playlistItem = item.getItem();

                        if (playlistItem instanceof Track spotifyTrack ){
                            String artist = spotifyTrack.getArtists().length > 0
                                    ? spotifyTrack.getArtists()[0].getName()
                                    : "Unknown";

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
        } catch (ForbiddenException e) {
            throw new RuntimeException(
                    "Acesso negado à playlist. "
                            + "Verifique as permissões da conta. "
                            + "Para ler as faixas, você precisa ser proprietário "
                            + "ou colaborador da playlist. "
                            + "Se ela pertence a outra pessoa, peça ao criador "
                            + "para adicioná-lo como colaborador.", e);

        }catch (InterruptedException e){
            Thread.currentThread().interrupt();

            throw new OperationInterruptedException(
                    "Leitura da playlist Spotify interrompida.", e);

        }catch (RuntimeException e){
            throw e;

        }catch (Exception e){
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
