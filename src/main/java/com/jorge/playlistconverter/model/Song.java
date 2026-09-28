package com.jorge.playlistconverter.model;

/**
 * Representa uma faixa de música, independente do serviço de origem.
 * O mesmo objeto é usado para faixas vindas do Spotify ou do YouTube Music.
 */
public record Song(
        String id,          // ID da faixa no serviço de origem
        String title,
        String artist,
        int durationMs
) {
}
