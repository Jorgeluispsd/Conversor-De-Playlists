package com.jorge.playlistconverter;

import com.jorge.playlistconverter.enums.SyncRunStatus;
import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.model.MatchResult;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.spotify.SpotifyAuthService;
import com.jorge.playlistconverter.spotify.SpotifyMusicService;
import com.jorge.playlistconverter.spotify.callback.SpotifyCallbackServer;
import com.jorge.playlistconverter.state.H2SyncStateStore;
import com.jorge.playlistconverter.youtube.YoutubeMusicService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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


                System.out.println("\n=== Faixas da primeira leitura ===");

                songs.forEach(song -> System.out.println(
                        "- " + song.title() + " - " + song.artist()
                ));


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
    CommandLineRunner run(H2SyncStateStore store){
        return args -> {
            String sourceId = "h2-teste-source-lookup";
            String destinationId = "h2-teste-destination-lookup";
            String direction = "SPOTIFY_TO_YOUTUBE";

            OptionalLong existing = store.findSyncJob(sourceId, destinationId, direction);

            System.out.println("Consulta inicial: " + existing);

            long jobID = existing.isPresent()
                    ? existing.getAsLong()
                    : store.createSyncJob(sourceId, destinationId, direction);

            OptionalLong found = store.findSyncJob(sourceId, destinationId, direction);

            System.out.println("ID utilizado: " + jobID);
            System.out.println("ID recuperado: " + found);

            System.out.println("Mesmo ID? " + (found.isPresent() && found.getAsLong() == jobID));

            OptionalLong differentDestination = store.findSyncJob(
                    sourceId, "h2-test-other-destination-lookup", direction);

            System.out.println("Outro destiono: " + differentDestination);
        };
    }
     */

    /*
    @Bean
    CommandLineRunner runner(H2SyncStateStore store){
        return args -> {
            String sourceId = "h2-teste-source-lookup";
            String destinationId = "h2-teste-destination-lookup";
            String direction = "SPOTIFY_TO_YOUTUBE";

            long firstId = store.getOrCreateSyncJob(sourceId, destinationId, direction);

            long secondId = store.getOrCreateSyncJob(sourceId, destinationId, direction);

            System.out.println("Primeiro ID: " + firstId);
            System.out.println("Segundo ID: " + secondId);
            System.out.println("Mesmo ID? " + (firstId == secondId));

            Song failedSong = new Song("h2-test-failed-track", "Faixa de teste com falha",
                    "Artista de teste", 200000);

            store.markSynced(firstId, failedSong, "h2-test-destination-track", "failed", 0.9);

            System.out.println("Registro com falha conta como sincronizado? " +
                    store.isAlreadySynced(firstId, failedSong.id()));

            store.markSynced(firstId, failedSong, "h2-test-destination-track", "success", 0.9);

            System.out.println("Após sucesso conta como sincronizado? " +
                    store.isAlreadySynced(firstId, failedSong.id()));
        };
    }

     */
/*
    @Bean
    CommandLineRunner runner(H2SyncStateStore store) {
        return args -> {

            String sourcePlaylistId = "h2-concurrency-" + UUID.randomUUID();
            String destinationPlaylistId = "h2-concurrency-destination";
            String direction = "SPOTIFY_TO_YOUTUBE";

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(() -> {
                    ready.countDown();
                    start.await();

                    return store.getOrCreateSyncJob(sourcePlaylistId, destinationPlaylistId, direction);
                });

                var second = executor.submit(() -> {
                    ready.countDown();
                    start.await();

                    return store.getOrCreateSyncJob(sourcePlaylistId, destinationPlaylistId, direction);
                });

                boolean bothReady = ready.await(5, TimeUnit.SECONDS);

                start.countDown();

                if (!bothReady) {
                    throw new IllegalStateException("As duas tarefas não ficaram prontas a tempo");
                }

                long firstId = first.get(15, TimeUnit.SECONDS);
                long secondId = second.get(15, TimeUnit.SECONDS);

                System.out.println("Origem utilizada: " + sourcePlaylistId);
                System.out.println("ID da primeira chamada: " + firstId);
                System.out.println("ID da segunda chamada: " + secondId);
                System.out.println("Mesmo ID? " + (firstId == secondId));
            }
        };
    }

 */

    /*
    @Bean
    CommandLineRunner runner(H2SyncStateStore store){
        return args -> {
            long jobId = store.getOrCreateSyncJob(
                    "h2-test-run-source-5",
                    "h2-test-run-destination",
                    "SPOTIFY_TO_YOUTUBE");


            long finishedRunId = store.startSyncRun(jobId);


            store.finishSyncRun(finishedRunId, SyncRunStatus.SUCCESS);

            try {
                store.finishSyncRun(finishedRunId, SyncRunStatus.FAILED);

                throw new AssertionError("O método permitiu finalizar duas vezes");

            }catch (IllegalStateException e){
                System.out.println("Segunda finalização rejeitada " + e.getMessage());
            }


            long openRunId = store.startSyncRun(jobId);

            try {
                store.finishSyncRun(openRunId, SyncRunStatus.RUNNING);

                throw new AssertionError("O método aceitou RUNNING como resultado");

            } catch (IllegalArgumentException e) {
                System.out.println("RUNNING REJEITADO " + e.getMessage());
            }

            try {
                store.finishSyncRun(openRunId, null);

                throw new AssertionError("O método aceitou um status nulo");

            } catch (NullPointerException e) {
                System.out.println("Status nulo rejeitado " + e.getMessage());
            }


            System.out.println("ID já finalizado: " + finishedRunId);
            System.out.println("ID ainda aberto: " + openRunId);
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
            Song exemplo = new Song("1", "Numb", "Linkin Park", 185_000);

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

            //System.out.println("\n ======== Testando Agora com Playlists =======");

            //List<Song> playlist = youtubeMusicService.getPlaylistTracks("PLNifpA8xogw8LxEb5dW5-_qX1uk9n0FMv");
            //System.out.println("Total de faixas encontradas: " + playlist.size());
            //playlist.forEach(t -> System.out.println(t.title() + " | " + t.artist()));

        };
    }
    */

}
