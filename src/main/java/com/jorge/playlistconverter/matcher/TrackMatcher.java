package com.jorge.playlistconverter.matcher;

import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Track;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.text.Normalizer;
import java.util.List;

/**
 * Fase 4 — o próximo passo que você quer atacar: dado uma faixa de origem e uma
 * lista de candidatos vindos da busca, escolher o melhor e calcular um score
 * de confiança. Nesta fase o objetivo é só IMPRIMIR o resultado no console
 * (ver TODO em MainRunner) para validar visualmente se está acertando.
 *
 * Sugestão de algoritmo inicial (ver conversa anterior sobre Levenshtein/Jaro-Winkler):
 * 1. Normalizar título e artista (minúsculo, remover acentos, remover parênteses
 *    tipo "(Official Video)", "(Lyrics)").
 * 2. Calcular similaridade de string entre o título normalizado da origem e o
 *    de cada candidato.
 * 3. Penalizar (ou descartar) candidatos com palavras como "cover", "live",
 *    "remix", "8D audio" quando a faixa original não tiver essas palavras.
 * 4. Retornar o candidato de maior score como MatchResult.
 */
public class TrackMatcher {

    private static final List<String> UNWANTED_WORDS = List.of(
            "cover", "live", "remix", "8d audio", "type beat", "instrumental");

    private static final double MIN_CONFIDENCE = 0.5;
    private static final double CONTAINMENT_FLOOR = 0.5;
    private static final int HASHTAG_SPAM_THRESHOLD = 2;

    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();

    public MatchResult findBestMatch(Track sourceTrack, List<Track> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new MatchResult(sourceTrack, null, null, 0.0);
        }

        String rawSourceTitle = sourceTrack.title().toLowerCase();
        String normalizedSourceTitle = normalizeText(sourceTrack.title());
        String normalizedSourceArtist = normalizeText(sourceTrack.artist());

        Track bestMatch = null;
        double bestScore = 0.0;

        for (Track candidate : candidates) {

            String rawCandidateTitle = candidate.title().toLowerCase();
            String normalizedCandidateTitle = normalizeText(candidate.title());
            String normalizedCandidateArtist = normalizeText(candidate.artist());

            double titleSimilarity = computeTitleSimilarity(normalizedSourceTitle, normalizedCandidateTitle);
            double artistSimilarity = jaroWinkler.apply(normalizedSourceArtist, normalizedCandidateArtist);

            double score = (titleSimilarity * 0.6) + (artistSimilarity * 0.4);

            score = applyPenalty(score, rawSourceTitle, rawCandidateTitle);
            score = applySpamPenalty(score, rawCandidateTitle);
            score = applyOfficialityBonus(score, rawCandidateTitle);
            score = Math.min(score, 1.0);

            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }

        if (bestScore < MIN_CONFIDENCE) {
            return new MatchResult(sourceTrack, null, null, 0.0);
        }

        return new MatchResult(sourceTrack, bestMatch.id(), bestMatch.title(), bestScore);
    }

    private double computeTitleSimilarity(String normalizedSourceTitle, String normalizedCandidateTitle) {

        double rawSimilarity = jaroWinkler.apply(normalizedSourceTitle, normalizedCandidateTitle);

        if (!normalizedSourceTitle.isBlank() && normalizedCandidateTitle.contains(normalizedSourceTitle))
            return Math.max(rawSimilarity, CONTAINMENT_FLOOR);

        return rawSimilarity;
    }


    private String normalizeText(String text) {
        if (text == null || text.isEmpty()) return "";

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        normalized = normalized.toLowerCase();
        normalized = normalized.replaceAll("#[\\w]+", "");
        normalized = normalized.replaceAll("\\([^)]*\\)", "");
        return normalized.trim().replaceAll("\\s+", " ");
    }

    private double applyPenalty(double score, String sourceTitle, String candidateTitle) {
        for (String keyword : UNWANTED_WORDS) {
            if (candidateTitle.contains(keyword) && !sourceTitle.contains(keyword)) {
                score *= 0.5;
            }
        }
        return score;
    }

    private double applySpamPenalty(double score, String rawCandidateTitle) {

        long hashtagCount = rawCandidateTitle.chars().filter(c -> c == '#' ).count();
        if (hashtagCount > HASHTAG_SPAM_THRESHOLD) {
            score *= 0.4;
        }
        return score;
    }

    private double applyOfficialityBonus(double score, String rawCanditdateTitle) {

        return rawCanditdateTitle.contains("official") ? score * 1.15 : score;
    }
}
