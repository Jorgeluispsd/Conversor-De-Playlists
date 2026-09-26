package com.jorge.playlistconverter;

import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Track;
import com.jorge.playlistconverter.youtube.YoutubeMusicService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class PlaylistConverterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlaylistConverterApplication.class, args);
    }

    /**
     * Runner temporário para a Fase 4: ler credenciais do .env e imprimir o
     * resultado do matching no console, sem gravar nada ainda.
     *
     * Substitua o conteúdo deste método conforme for avançando pelas fases:
     * Fase 1-2 -> autenticar no Spotify e listar as faixas de uma playlist real
     * Fase 3   -> buscar candidatos no YouTube para cada faixa
     * Fase 4   -> passar (faixa, candidatos) para o TrackMatcher e printar o resultado
     */
    @Bean
    CommandLineRunner run(YoutubeMusicService youtubeMusicService) {
        return args -> {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

            System.out.println("=== Conversor de Playlists ===");
            System.out.println("SPOTIFY_CLIENT_ID carregado: "
                    + (dotenv.get("SPOTIFY_CLIENT_ID") != null && !dotenv.get("SPOTIFY_CLIENT_ID").isBlank()));
            System.out.println("YOUTUBE_API_KEY carregado: "
                    + (dotenv.get("YOUTUBE_API_KEY") != null && !dotenv.get("YOUTUBE_API_KEY").isBlank()));

            // TODO (Fase 2): trocar por faixas reais vindas do SpotifyMusicService
            Track exemplo = new Track("1", "Blinding Lights", "The Weeknd", 200000);

            // TODO (Fase 4): trocar por candidatos reais vindos do YoutubeMusicService
            List<Track> candidatos = youtubeMusicService.searchCandidates(exemplo);

            System.out.println("=== Candidatos encontrados: " + candidatos.size() + " ===");
            candidatos.forEach(c -> System.out.println("- " + c.title() + " | " + c.artist()));

            //List<Track> candidatosFicticios = List.of();

            TrackMatcher matcher = new TrackMatcher();
            MatchResult resultado = matcher.findBestMatch(exemplo, candidatos);

            System.out.println();

            System.out.printf("Origem: %s - %s | Encontrado: %s | Confiança: %.2f%n",
                    exemplo.artist(), exemplo.title(),
                    resultado.found() ? resultado.matchedTitle() : "NÃO ENCONTRADO",
                    resultado.confidence());
        };
    }
}
