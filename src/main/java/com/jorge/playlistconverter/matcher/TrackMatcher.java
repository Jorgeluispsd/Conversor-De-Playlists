package com.jorge.playlistconverter.matcher;

import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.text.Normalizer;
import java.util.List;

/**
 * Compara títulos e artistas normalizados usando Jaro-Winkler, com penalidades
 * para versões diferentes e ajustes para os metadados dos candidatos.
 * Retorna o melhor resultado conforme o limiar atual, sem chamadas às APIs nem escrita.
 * A pontuação indica semelhança; revisão e confirmação do usuário pertencem à Fase 8.
 */
public class TrackMatcher {

    private static final List<String> UNWANTED_WORDS = List.of(
            "cover", "live", "remix", "8d audio", "type beat", "instrumental");

    private static final double MIN_CONFIDENCE = 0.5;
    private static final double CONTAINMENT_FLOOR = 0.5;
    private static final int HASHTAG_SPAM_THRESHOLD = 2;

    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();

    public MatchResult findBestMatch(Song sourceSong, List<Song> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new MatchResult(sourceSong, null, null, 0.0);
        }

        String rawSourceTitle = sourceSong.title().toLowerCase();
        String normalizedSourceTitle = normalizeText(sourceSong.title());
        String normalizedSourceArtist = normalizeText(sourceSong.artist());

        Song bestMatch = null;
        double bestScore = 0.0;

        for (Song candidate : candidates) {

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
            return new MatchResult(sourceSong, null, null, 0.0);
        }

        return new MatchResult(sourceSong, bestMatch.id(), bestMatch.title(), bestScore);
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

    private double applyOfficialityBonus(double score, String rawCandidateTitle) {

        return rawCandidateTitle.contains("official") ? score * 1.15 : score;
    }
}
