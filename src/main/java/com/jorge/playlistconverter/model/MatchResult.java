package com.jorge.playlistconverter.model;

/**
 * Resultado de tentar encontrar, no serviço de destino, a faixa equivalente
 * a uma faixa da playlist de origem.
 *
 * Na Fase 4 do projeto, isso é usado só para IMPRIMIR no console (sourceTrack
 * vs. candidato encontrado + confidence), antes de gravar qualquer coisa.
 */
public record MatchResult(
        Song sourceSong,
        String matchedId,       // null se não achou nenhum candidato
        String matchedTitle,    // título retornado pela busca, para você comparar visualmente
        double confidence       // 0.0 a 1.0 — TODO: calcular via similaridade de string (Fase 4)
) {
    public boolean found() {
        return matchedId != null;
    }
}
