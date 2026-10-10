package com.jorge.playlistconverter.youtube;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.jorge.playlistconverter.errors.YoutubePlaylistReadException;
import com.jorge.playlistconverter.errors.YoutubeSearchException;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fase 3: implementar searchCandidates usando search.list (chave de API simples,
 * sem OAuth — leitura/busca não exige login de usuário).
 * Fase 6: implementar createPlaylist / addTracks (aqui sim exige OAuth de usuário,
 * porque é uma escrita na conta dele).
 * Fase 7: implementar getPlaylistTracks (para a conversão inversa).
 */
public class YoutubeMusicService implements MusicService {

    private final YouTube youtube;
    private final String apiKey;

    public YoutubeMusicService(YouTube youtube, String apiKey) {
        this.youtube = youtube;
        this.apiKey = apiKey;
    }

    @Override
    public List<Song> getPlaylistTracks(String playlistId) {
        // TODO (Fase 7): usar playlistItems.list
        try {
            List<Song> songs = new ArrayList<>();
            String pageToken = null;

            do {
                YouTube.PlaylistItems.List request = youtube.playlistItems()
                        .list(List.of("snippet"))
                        .setPlaylistId(playlistId)
                        .setMaxResults(50L)
                        .setKey(apiKey);

                if (pageToken != null) {
                    request.setPageToken(pageToken);
                }

                PlaylistItemListResponse response = request.execute();

                for (PlaylistItem item : response.getItems()) {
                    songs.add(new Song(
                            item.getSnippet().getResourceId().getVideoId(),
                            item.getSnippet().getTitle(),
                            item.getSnippet().getVideoOwnerChannelTitle(),
                            0
                    ));
                }

                pageToken = response.getNextPageToken();

            } while (pageToken != null);

            return songs;

        }catch (GoogleJsonResponseException e){
            throw new YoutubePlaylistReadException("A API do YouTube recusou a leitura da playlist. "
             + "Código HTTP: " + e.getStatusCode() + ".", e);

        }catch (IOException e){
            throw new YoutubePlaylistReadException(
                    "Falha de comunicação ao consultar a playlist do YouTube.", e);
        }
        //throw new UnsupportedOperationException("TODO: Fase 7 (conversão inversa)");
    }

    @Override
    public List<Song> searchCandidates(Song sourceSong) {
        // TODO (Fase 3): montar query "artista + título" e chamar youtube.search().list(...)
        String query = sourceSong.artist() + " " + sourceSong.title();

        try {

            YouTube.Search.List request = youtube.search()
                    .list(List.of("snippet"))
                    .setQ(query)
                    .setType(List.of("video"))
                    .setMaxResults(5L)
                    .setKey(apiKey);

            SearchListResponse response = request.execute();

            return response.getItems().stream()
                    .map(item -> new Song(
                            item.getId().getVideoId(),
                            item.getSnippet().getTitle(),
                            item.getSnippet().getChannelTitle(),
                            0
                    ))
                    .toList();

        }catch (GoogleJsonResponseException e){
            throw new YoutubeSearchException(
                    "A API do YouTube recusou a busca de candidatos. "
                            + "Código HTTP: " + e.getStatusCode() + ".", e);

        } catch (IOException e) {
            throw new YoutubeSearchException("Falha de comunicação ao buscar candidatos no YouTube.", e);
        }
        // Dica: peça uns 3-5 resultados por busca, não só o primeiro — o TrackMatcher
        // (Fase 4) vai escolher o melhor entre eles.
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
        // TODO (Fase 5)
        throw new UnsupportedOperationException("TODO: Fase 5");
    }
}
