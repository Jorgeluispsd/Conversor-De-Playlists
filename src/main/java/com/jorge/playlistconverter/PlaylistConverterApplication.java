package com.jorge.playlistconverter;

import com.jorge.playlistconverter.converter.ConversionResult;
import com.jorge.playlistconverter.converter.PlaylistConverter;
import com.jorge.playlistconverter.converter.PlaylistPreview;
import com.jorge.playlistconverter.enums.PlaylistVisibility;
import com.jorge.playlistconverter.model.Song;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.jorge.playlistconverter.input.SpotifyPlaylistParser;
import com.jorge.playlistconverter.input.YoutubePlaylistParser;

import java.util.List;
import java.util.Scanner;


@SpringBootApplication
public class PlaylistConverterApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PlaylistConverterApplication.class);

        application.setHeadless(false);
        application.run(args);
    }

    /**
     * Laboratório de validações manuais de Spotify, YouTube, matcher e persistência H2.
     * Habilitar apenas os runners do cenário em teste; ainda não realiza conversão completa.
     * TODO (Fase 6): validar escrita e orquestração primeiro com playlist pequena.
     * TODO (Fase 8): expor análise, prévia, revisão e execução confirmada por interface web.
     */

/*
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
 */



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

            // Exemplo manual de uma faixa; a leitura de playlist Spotify já existe em outro runner.
            Song exemplo = new Song("1", "Numb", "Linkin Park", 185_000);

            // Consulta real ao YouTube: cada execução consome uma pesquisa da quota do projeto.
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

    /*
    @Bean
    CommandLineRunner desktopDiagnosticRunner(){
        return args -> {
            boolean headless = GraphicsEnvironment.isHeadless();
            boolean desktopSupported = Desktop.isDesktopSupported();

            boolean browseSupported = desktopSupported
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);

            System.out.println("=== Diagnóstico do navegador ===");
            System.out.println("java.awt.headless: "
                    + System.getProperty("java.awt.headless"));
            System.out.println("Ambiente headless: " + headless);
            System.out.println("Desktop disponível: " + desktopSupported);
            System.out.println("BROWSE disponível: " + browseSupported);
        };
    }

     */

    /*
    @Bean
    CommandLineRunner googleCredentialsValidationRunner(
            GoogleClientSecretsLoader loader
    ) {
        return args -> {
            loader.load();

            System.out.println(
                    "Credenciais OAuth Google carregadas e validadas.");
        };
    }

     */

    /*
    @Bean
    CommandLineRunner googleLoginRunner(
            GoogleAuthService authService,
            GoogleAuthorizationFlowFactory flowFactory
    ) {
        return args -> {
            System.out.println("=== Teste de login Google ===");

            Credential credential = authService.ensureAuthenticated();

            boolean hasAccessToken =
                    credential.getAccessToken() != null
                            && !credential.getAccessToken().isBlank();

            boolean hasRefreshToken =
                    credential.getRefreshToken() != null
                            && !credential.getRefreshToken().isBlank();

            System.out.println("Access token recebido? " + hasAccessToken);
            System.out.println("Refresh token recebido? " + hasRefreshToken);

            Credential savedCredential =
                    flowFactory.create().loadCredential("local-user");

            System.out.println(
                    "Sessão recuperada do armazenamento? "
                            + (savedCredential != null));
        };
    }

     */

    /*
    @Bean
    CommandLineRunner googleAuthenticatedRequestRunner(GoogleAuthService authService){
        return args ->{
            System.out.println("=== Consulta autenticada ao YouTube ===");

            Credential credential = authService.ensureAuthenticated();

            YouTube authenticatedYoutube = new YouTube.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("playlist-converter")
                    .build();

            ChannelListResponse response;

            try{
                response = authenticatedYoutube.channels()
                        .list(List.of("snippet"))
                        .setMine(true).
                        execute();

            }catch (GoogleJsonResponseException e){
                System.out.println(
                        "YouTube recusou a consulta. Código HTTP: "
                                + e.getStatusCode());

                return;

            } catch (IOException e) {
                System.out.println(
                        "Não foi possível concluir a comunicação com o YouTube.");

                return;
            }

            System.out.println("Consulta autenticada aceita pelo YouTube.");

            if (response.getItems() == null || response.getItems().isEmpty()) {
                System.out.println(
                        "A consulta não retornou um canal para a conta autorizada.");

                return;
            }

            response.getItems().forEach(channel -> System.out.println(
                    "Canal autorizado: " + channel.getSnippet().getTitle()));
        };

    }

     */
/*
    @Bean
    CommandLineRunner youtubePlaylistCreationRunner(YoutubeMusicService youtubeMusicService){
        return args -> {
            System.out.println("=== Criação de playlist de teste ===");

            String playlistId = youtubeMusicService.createPlaylist(
                    "Rockzinho da massa", YoutubePlaylistVisibility.PRIVATE);

            System.out.println("Playlist privada criada.");
            System.out.println("ID: " + playlistId);
            System.out.println("Link: https://www.youtube.com/playlist?list=" + playlistId);
        };
    }

 */



/*
    @Bean
    CommandLineRunner youtubeTrackInsertionRunner(YoutubeMusicService youtubeMusicService){
        return args -> {
            String playlistId = "PLfRsEUW_s_1I";

            List<String> videoIds = List.of(
                    "ocpDEOXABWg",
                    "fcsDG_jVYbc");

            System.out.println("=== Inserção de vídeos de teste ===");

            youtubeMusicService.addTracks(playlistId, videoIds);

            System.out.println(
                    "Inserções confirmadas: " + videoIds.size());

            System.out.println(
                    "Confira a ordem na playlist: "
                            + "https://www.youtube.com/playlist?list="
                            + playlistId);
        };
    }


 */
/*
    @Bean
    CommandLineRunner existingPlaylistPreviewRunner(
            PlaylistConverter converter,
            SpotifyPlaylistParser spotifyPlaylistParser,
            YoutubePlaylistParser youtubePlaylistParser
    ) {
        return args -> {
            String sourcePlaylistInput = "https://open.spotify.com/playlist/2XnKAdcompOGmEpQnngIyx";
            String destinationPlaylistInput = "https://www.youtube.com/playlist?list=PLVMPlT659QZY";

            String sourcePlaylistId = spotifyPlaylistParser.parse(sourcePlaylistInput);
            String destinationPlaylistId = youtubePlaylistParser.parse(destinationPlaylistInput);

            PlaylistPreview preview =
                    converter.previewExistingPlaylist(
                            sourcePlaylistId,
                            destinationPlaylistId);

            List<Song> notFoundSongs = preview.notFoundSongs();

            System.out.println("=== Prévia Spotify → YouTube ===");
            System.out.println("Total: " + preview.totalCount());
            System.out.println("Encontradas: " + preview.foundCount());

            System.out.println(
                    "Já presentes no destino: " + preview.alreadyPresentCount());

            System.out.println(
                    "Novas para adicionar: " + preview.newTrackCount());

            System.out.println(
                    "Candidatos novos repetidos: " + preview.repeatedCandidateCount());

            System.out.println(
                    "Sem correspondência: " + notFoundSongs.size());

            System.out.println("\n=== Não encontradas ===");
            notFoundSongs.forEach(song ->
                    System.out.println(
                            "- " + song.title() + " — " + song.artist()));

            System.out.println("\n=== Correspondências selecionadas ===");

            preview.matches().stream()
                    .filter(match -> match.found())
                    .forEach(match -> System.out.printf(
                            "%s → %s | ID: %s | Já presente: %s%n",
                            match.sourceSong().title(),
                            match.matchedTitle(),
                            match.matchedId(),
                            preview.existingDestinationIds()
                                    .contains(match.matchedId())));

            System.out.println(
                    "\nIDs já presentes no destino: "
                            + preview.existingDestinationIds());

            System.out.println("\nPrévia concluída. Nenhuma escrita foi solicitada.");

            System.out.println(
                    "Destino: https://www.youtube.com/playlist?list="
                            + preview.destinationPlaylistId());

            System.out.println(
                    "Digite CONFIRMAR para inserir os novos vídeos "
                            + "ou qualquer outro texto para cancelar:");

            Scanner input = new Scanner(System.in);

            if (!input.hasNextLine()) {
                System.out.println("Sem confirmação. Nenhum vídeo foi inserido.");
                return;
            }

            String answer = input.nextLine().strip();

            if (!"CONFIRMAR".equalsIgnoreCase(answer)) {
                System.out.println("Conversão cancelada. Nenhum vídeo foi inserido.");
                return;
            }

            ConversionResult result =
                    converter.executeExistingPlaylistPreview(preview);

            System.out.println("\n=== Resultado da conversão ===");
            System.out.println("Inserções confirmadas: " + result.insertedCount());
            System.out.println("Itens pulados: " + result.skippedCount());

            System.out.println(
                    "Playlist: https://www.youtube.com/playlist?list="
                            + result.destinationPlaylistId());
        };
    }

 */


    @Bean
    CommandLineRunner newPlaylistPreviewRunner(PlaylistConverter converter,
                                               SpotifyPlaylistParser spotifyPlaylistParser) {
        return args -> {
            String sourcePlaylistInput = "https://open.spotify.com/playlist/2XnKAdcompOGmEpQnngIyx";

            String sourcePlaylistId = spotifyPlaylistParser.parse(sourcePlaylistInput);
            String destinationName = "Playlist Converter - pedro";
            PlaylistVisibility visibility = PlaylistVisibility.PUBLIC;

            PlaylistPreview preview =
                    converter.previewNewPlaylist(sourcePlaylistId);

            System.out.println("=== Prévia Spotify → nova playlist YouTube ===");
            System.out.println("Nome do destino: " + destinationName);
            System.out.println("Visibilidade: " + visibility);
            System.out.println("Total na origem: " + preview.totalCount());
            System.out.println("Encontradas: " + preview.foundCount());
            System.out.println("Para adicionar: " + preview.newTrackCount());
            System.out.println(
                    "Candidatos repetidos: " + preview.repeatedCandidateCount());

            System.out.println("\n=== Correspondências selecionadas ===");

            preview.matches().stream()
                    .filter(match -> match.found())
                    .forEach(match -> System.out.printf(
                            "%s → %s | ID: %s%n",
                            match.sourceSong().title(),
                            match.matchedTitle(),
                            match.matchedId()));

            System.out.println(
                    "\n=== Não encontradas: "
                            + preview.notFoundSongs().size() + " ===");

            preview.notFoundSongs().forEach(song -> System.out.printf(
                    "- %s — %s%n", song.title(),
                    song.artist()));

            if (preview.newTrackCount() == 0) {
                System.out.println(
                        "Nenhuma música disponível para inserir. "
                                + "A playlist não será criada.");
                return;
            }

            System.out.println(
                    "\nDigite CONFIRMAR para criar a playlist e inserir "
                            + "as músicas, ou qualquer outro texto para cancelar:");

            Scanner input = new Scanner(System.in);

            if (!input.hasNextLine()
                    || !"CONFIRMAR".equalsIgnoreCase(input.nextLine().strip())) {
                System.out.println(
                        "Conversão cancelada. Nenhuma playlist foi criada.");
                return;
            }

            ConversionResult result = converter.executeNewPlaylistPreview(
                    preview,
                    destinationName,
                    visibility);

            System.out.println("\n=== Conversão concluída ===");
            System.out.println("Adicionadas: " + result.insertedCount());
            System.out.println("Repetidas puladas: " + result.skippedCount());
            System.out.println("Link: https://www.youtube.com/playlist?list="
                            + result.destinationPlaylistId());
        };
    }

}
