package com.jorge.playlistconverter.youtube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.jorge.playlistconverter.model.Track;
import com.jorge.playlistconverter.service.MusicService;

import java.io.IOException;
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
    public List<Track> getPlaylistTracks(String playlistId) {
        // TODO (Fase 7): usar playlistItems.list
        throw new UnsupportedOperationException("TODO: Fase 7 (conversão inversa)");
    }

    @Override
    public List<Track> searchCandidates(Track sourceTrack) {
        // TODO (Fase 3): montar query "artista + título" e chamar youtube.search().list(...)
        String query = sourceTrack.artist() + " " + sourceTrack.title();

        try{

            YouTube.Search.List request = youtube.search()
                    .list(List.of("snippet"))
                    .setQ(query)
                    .setType(List.of("video"))
                    .setMaxResults(5L)
                    .setKey(apiKey);

            SearchListResponse response = request.execute();

            return response.getItems().stream()
                    .map(item -> new Track(
                            item.getId().getVideoId(),
                            item.getSnippet().getTitle(),
                            item.getSnippet().getChannelTitle(),
                            0
                    ))
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Erro ao buscar video no youtube" + query , e);
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
    public List<Track> getPlaylistTracksAlreadyAdded(String playlistId) {
        // TODO (Fase 5)
        throw new UnsupportedOperationException("TODO: Fase 5");
    }
}
