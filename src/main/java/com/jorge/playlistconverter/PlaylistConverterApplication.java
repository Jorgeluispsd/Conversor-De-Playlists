package com.jorge.playlistconverter;

import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.spotify.SpotifyMusicService;
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
    CommandLineRunner run(SpotifyMusicService spotifyMusicService){
        return args -> {
            try {
                String playlistId = "0ulqoAeaiNTF5YTKQ4nD9P";


                System.out.println("=== Primeira leitura ===");

                List<Song> songs =
                        spotifyMusicService.getPlaylistTracks(playlistId);

                System.out.println("Total de faixas: " + songs.size());

                System.out.println("\n=== Segunda leitura ===");

                List<Song> songsAgain =
                        spotifyMusicService.getPlaylistTracks(playlistId);

                System.out.println("Total de faixas: " + songsAgain.size());

                /*
                System.out.println("=== Buscando faixas da playlist ===");
                List<Song> songs = spotifyMusicService.getPlaylistTracks(playlistId);

                System.out.println("\nTotal de faixas: " + songs.size());
                songs.forEach(song -> System.out.println("- " + song.title() + " - " + song.artist()));

                 */

            } catch (Exception e) {
                System.out.println("Erro durante o teste: " + e.getMessage());

                e.printStackTrace();
            }
        };
    }




    /*
    @Bean
    CommandLineRunner run(H2SyncStateStore syncStateStore){
        return args -> {
          String trackId = "test-track-123";


            System.out.println("=== Teste 0: Criando sync_job de teste ===");
            long syncJobId = syncStateStore.createSyncJob(
                    "test-source-playlist", "test-destination-playlist", "SPOTIFY_TO_YOUTUBE");
            System.out.println("sync_job criado com id: " + syncJobId);

            Song testSong = new Song(trackId, "Test Song", "Test Artist", 200000);

            System.out.println("=== Teste 1: Marcando faixa como sincronizada ===");
            syncStateStore.markSynced(syncJobId, testSong, "dest-123", "success", 0.95);
            System.out.println("Faixa marcada");

            boolean isSynced = syncStateStore.isAlreadySynced(syncJobId, trackId);
            System.out.println("Faixa já sincronizada? " + isSynced);

            System.out.println("=== Teste 2: Verificando novamente ===");
            isSynced = syncStateStore.isAlreadySynced(syncJobId, trackId);
            System.out.println("Faixa ainda sincronizada? " + isSynced);

            System.out.println("=== Teste 3: Histórico ===");
            List<String> history = syncStateStore.getHistory(syncJobId);
            System.out.println("Histórico: " + history);

            System.out.println("=== Teste concluído com sucesso! ===");
        };
    }

     */

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

            //List<Song> candidatosFicticios = List.of();

            TrackMatcher matcher = new TrackMatcher();
            MatchResult resultado = matcher.findBestMatch(exemplo, candidatos);

            System.out.println();

            System.out.printf("Origem: %s - %s | Encontrado: %s | Confiança: %.2f%n",
                    exemplo.artist(), exemplo.title(),
                    resultado.found() ? resultado.matchedTitle() : "NÃO ENCONTRADO",
                    resultado.confidence());

            System.out.println("\n ======== Testando Agora com Playlists =======");

            List<Song> playlist = youtubeMusicService.getPlaylistTracks("PLNifpA8xogw8LxEb5dW5-_qX1uk9n0FMv");
            System.out.println("Total de faixas encontradas: " + playlist.size());
            playlist.forEach(t -> System.out.println(t.title() + " | " + t.artist()));

        };
    }


 */

}
