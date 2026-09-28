package com.jorge.playlistconverter;

import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.spotify.SpotifyAuthService;
import com.jorge.playlistconverter.spotify.SpotifyClientCredentialsService;
import com.jorge.playlistconverter.spotify.SpotifyMusicService;
import com.jorge.playlistconverter.spotify.SpotifyTokenStorage;
import com.jorge.playlistconverter.youtube.YoutubeMusicService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import se.michaelthelin.spotify.SpotifyApi;

import java.net.URI;
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
    public Dotenv dotenv() {
        return Dotenv.configure().load();
    }

    @Bean
    public SpotifyApi spotifyReadApi(Dotenv dotenv) {
        return new SpotifyApi.Builder()
                .setClientId(dotenv.get("SPOTIFY_CLIENT_ID"))
                .setClientSecret(dotenv.get("SPOTIFY_CLIENT_SECRET"))
                .build();
    }

    @Bean
    public SpotifyClientCredentialsService spotifyClientCredentialsService(SpotifyApi spotifyReadApi) {
        return new SpotifyClientCredentialsService(spotifyReadApi);
    }

    @Bean
    public SpotifyApi spotifyWriteApi(Dotenv dotenv){
        return new SpotifyApi.Builder()
                .setClientId(dotenv.get("SPOTIFY_CLIENT_ID"))
                .setClientSecret(dotenv.get("SPOTIFY_CLIENT_SECRET"))
                .setRedirectUri(URI.create(SpotifyAuthService.CALLBACK_URI))
                .build();
    }

    @Bean
    public SpotifyMusicService spotifyMusicService(SpotifyApi spotifyReadApi,
                                                   SpotifyApi spotifyWriteApi,
                                                   SpotifyAuthService spotifyAuthService) {
        return new SpotifyMusicService(spotifyReadApi, spotifyWriteApi, spotifyAuthService);
    }


    @Bean
    CommandLineRunner run(SpotifyClientCredentialsService spotifyClientCredentialsService,
                          SpotifyMusicService spotifyMusicService){
        return args -> {
            try {
                System.out.println("=== Iniciando autenticação Spotify ===");
                spotifyClientCredentialsService.ensureAuthenticated();
                System.out.println("=== Autenticação realizada com sucesso! ===");

                String playlistId = "0ulqoAeaiNTF5YTKQ4nD9P";

                System.out.println("=== Buscando faixas da playlist ===");
                List<Song> songs = spotifyMusicService.getPlaylistTracks(playlistId);
                System.out.println("=== Faixas listadas com sucesso! ===");

                System.out.println("Total de faixas: " + songs.size());
                songs.forEach(song -> System.out.println("- " + song.title() + " - " + song.artist()));

            } catch (Exception e) {
                System.out.println("Erro ao autenticar no Spotify: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }


    /*
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
            Song exemplo = new Song("1", "Surto", "Realygust", 200000);

            // TODO (Fase 4): trocar por candidatos reais vindos do YoutubeMusicService
            List<Song> candidatos = youtubeMusicService.searchCandidates(exemplo);

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

            System.out.println("\n ======== Testando Agora com Playlists =======");

            //List<Track> playlist = youtubeMusicService.getPlaylistTracks("PLxRW_UC-zxu2LtJHW7zKxWuyPf4wbmNLn");
            //System.out.println("Total de faixas encontradas: " + playlist.size());
            //playlist.forEach(t -> System.out.println(t.title() + " | " + t.artist()));

        };
    }

     */
}
