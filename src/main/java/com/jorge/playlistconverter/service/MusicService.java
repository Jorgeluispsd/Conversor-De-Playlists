package com.jorge.playlistconverter.service;

import com.jorge.playlistconverter.model.Track;

import java.util.List;
import java.util.Optional;

/**
 * Contrato comum entre Spotify e YouTube Music.
 * O PlaylistConverter (a ser criado na Fase 6) só enxerga esta interface —
 * não sabe nem se importa se está falando com Spotify ou YouTube. Isso é o
 * que permite a conversão nos dois sentidos sem duplicar lógica.
 */
public interface MusicService {

    // Fase 2 (Spotify) / Fase 7 (YouTube)
    List<Track> getPlaylistTracks(String playlistId);

    // Fase 3-4: buscar um candidato equivalente no serviço.
    // Por enquanto retorna Optional<Track> só do melhor resultado bruto da API,
    // sem cálculo de similaridade ainda — isso fica a cargo do TrackMatcher.
    List<Track> searchCandidates(Track sourceTrack);

    // Fase 6: criação real da playlist de destino
    String createPlaylist(String name);

    void addTracks(String playlistId, List<String> trackIds);

    // Fase 5-6: usado para checar duplicata antes de adicionar
    List<Track> getPlaylistTracksAlreadyAdded(String playlistId);
}
