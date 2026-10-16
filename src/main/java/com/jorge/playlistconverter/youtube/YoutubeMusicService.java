package com.jorge.playlistconverter.youtube;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.jorge.playlistconverter.errors.youtube.YoutubePlaylistCreationException;
import com.jorge.playlistconverter.errors.youtube.YoutubePlaylistReadException;
import com.jorge.playlistconverter.errors.youtube.YoutubeQuotaExceededException;
import com.jorge.playlistconverter.errors.youtube.YoutubeSearchException;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.service.MusicService;
import com.jorge.playlistconverter.youtube.authorization.YoutubeAuthenticatedClientFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Leitura de playlists e busca de candidatos no YouTube usando chave de API.
 * Quota esgotada é identificada por quotaExceeded, sem repetição imediata.
 * Fase 6: adicionar OAuth do usuário para criar playlists e inserir vídeos no destino.
 */
public class YoutubeMusicService implements MusicService {

    private final YouTube youtube;
    private final String apiKey;
    private final YoutubeAuthenticatedClientFactory authenticatedClientFactory;

    public YoutubeMusicService(YouTube youtube, String apiKey,
                               YoutubeAuthenticatedClientFactory authenticatedClientFactory) {
        this.youtube = youtube;
        this.apiKey = apiKey;
        this.authenticatedClientFactory = authenticatedClientFactory;
    }

    @Override
    public List<Song> getPlaylistTracks(String playlistId) {
        // Leitura implementada com playlistItems.list e nextPageToken, em páginas de até 50 itens.
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
            if (isQuotaExceeded(e)){
                throw new YoutubeQuotaExceededException("A quota da API do YouTube foi esgotada. "
                        + "A leitura da playlist não foi concluída. "
                        + "Consulte a quota do projeto antes de tentar novamente.", e);
            }

            throw new YoutubePlaylistReadException("A API do YouTube recusou a leitura da playlist. "
             + "Código HTTP: " + e.getStatusCode() + ".", e);

        }catch (IOException e){
            throw new YoutubePlaylistReadException(
                    "Falha de comunicação ao consultar a playlist do YouTube.", e);
        }

    }

    @Override
    public List<Song> searchCandidates(Song sourceSong) {
        // Busca implementada: uma chamada search.list por faixa, retornando até cinco candidatos.
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
            if (isQuotaExceeded(e)){
                throw new YoutubeQuotaExceededException("A quota da API do YouTube foi esgotada. "
                        + "A busca de candidatos não foi concluída. "
                        + "Consulte a quota do projeto antes de tentar novamente.", e);
            }

            throw new YoutubeSearchException(
                    "A API do YouTube recusou a busca de candidatos. "
                            + "Código HTTP: " + e.getStatusCode() + ".", e);

        } catch (IOException e) {
            throw new YoutubeSearchException("Falha de comunicação ao buscar candidatos no YouTube.", e);
        }
        // Os candidatos retornados são comparados pelo TrackMatcher fora deste serviço.
    }

    private boolean isQuotaExceeded(GoogleJsonResponseException exception){
        GoogleJsonError details = exception.getDetails();

        if (details == null || details.getErrors() == null){
            return false;
        }

        return details.getErrors().stream()
                .anyMatch(error -> error != null && "quotaExceeded".equals(error.getReason()));
    }

    @Override
    public String createPlaylist(String name) {
        if (name == null || name.isBlank()){
            throw new IllegalArgumentException("O nome da playlist deve ser informado.");
        }

        YouTube authenticatedYoutube = authenticatedClientFactory.create();

        Playlist playlist = new Playlist()
                .setSnippet(new PlaylistSnippet().setTitle(name.strip()))
                .setStatus(new PlaylistStatus().setPrivacyStatus("private"));

        try {
            Playlist created = authenticatedYoutube.playlists()
                    .insert(List.of("snippet", "status"), playlist)
                    .execute();

            if (created == null
                    || created.getId() == null
                    || created.getId().isBlank()){
                throw new YoutubePlaylistCreationException("O YouTube não retornou um ID válido "
                        + "após a solicitação de criação da playlist.");
            }

            return created.getId();

        }catch (GoogleJsonResponseException e){
            if (isQuotaExceeded(e)){
                throw new YoutubeQuotaExceededException(
                        "A quota da API do YouTube foi esgotada. "
                                + "Não foi possível concluir a criação da playlist.", e);
            }

            throw new YoutubePlaylistCreationException(
                    "A API do YouTube recusou a criação da playlist. "
                            + "Código HTTP: " + e.getStatusCode() + ".", e);

        }catch (IOException e){
            throw new YoutubePlaylistCreationException(
                    "Falha de comunicação durante a criação da playlist. "
                            + "Verifique o destino antes de tentar novamente.", e);
        }
    }

    @Override
    public void addTracks(String playlistId, List<String> trackIds) {
        // TODO (Fase 6): inserir videoIds com OAuth e playlistItems.insert, preservando ordem e tratando quota/falhas parciais.
        throw new UnsupportedOperationException("TODO: Fase 6");
    }

    @Override
    public List<Song> getPlaylistTracksAlreadyAdded(String playlistId) {
        // TODO (Fase 6): consultar itens já presentes no destino com getPlaylistTracks e coordenar duplicatas com o histórico H2.
        throw new UnsupportedOperationException("TODO: Fase 5");
    }
}
