# Tasks — Conversor de Playlists

Checklist de desenvolvimento, na ordem recomendada. Marque cada item conforme for
concluindo. As referências de arquivo/classe já existem no esqueleto do projeto.

---

## Fase 0 — Setup (feito, só confirme)

- [X] Importar o projeto no IntelliJ como projeto Maven (abrir a pasta, o IntelliJ
  detecta o `../pom.xml` sozinho).
- [X] Copiar `.env.example` para `.env` na raiz do projeto.
- [X] Rodar `mvn clean install` e confirmar que builda sem erro.
- [X] Rodar `PlaylistConverterApplication` e confirmar no console que aparece
      "SPOTIFY_CLIENT_ID carregado: false" / "YOUTUBE_API_KEY carregado: false"
      (false porque o `.env` ainda está vazio — é esperado).

---

## Fase 1 — Autenticação Spotify

- [ ] Criar app em https://developer.spotify.com/dashboard.
- [ ] Em *Users Management*, adicionar seu próprio e-mail Spotify como usuário
      autorizado (obrigatório em Development Mode).
- [ ] Copiar Client ID e Client Secret para o `.env`.
- [ ] Criar classe `com.jorge.playlistconverter.spotify.SpotifyAuthService`
      (nova, não existe ainda) responsável por:
  - [ ] Montar a URL de autorização (Authorization Code + PKCE, escopo
        `playlist-read-private`).
  - [ ] Abrir essa URL no navegador padrão.
  - [ ] Subir um servidor HTTP local simples (ex: `com.sun.net.httpserver.HttpServer`)
        só para capturar o `code` que o Spotify devolve no redirect.
  - [ ] Trocar o `code` por um `access_token` + `refresh_token` via
        `spotifyApi.authorizationCodePKCERequest(...)`.
- [ ] Testar manualmente: rodar o fluxo e confirmar no console que recebeu um
      access token (não precisa usá-lo ainda).

---

## Fase 2 — Listar faixas de uma playlist do Spotify

- [ ] Em `SpotifyMusicService`, implementar `getPlaylistTracks(String playlistId)`:
  - [ ] Chamar `spotifyApi.getPlaylistsItems(playlistId)` (ou equivalente na
        versão 7.0.0 da lib).
  - [ ] Tratar paginação — a API retorna no máximo 100 itens por página; repetir
        a chamada com `offset` até não haver mais itens.
  - [ ] Mapear cada item para um `Track` (título, artista principal, duração).
- [ ] Atualizar `PlaylistConverterApplication.run()`: trocar o `Track exemplo`
      fixo por uma chamada real a `spotifyMusicService.getPlaylistTracks(...)`
      usando o ID de uma playlist sua de teste.
- [ ] Rodar e conferir no console se a lista impressa bate com a playlist real.

---

## Fase 3 — Buscar candidatos no YouTube

- [X] Criar projeto no Google Cloud Console, ativar a **YouTube Data API v3**,
      gerar uma **API key** (não precisa de OAuth para busca).
- [X] Copiar a chave para `YOUTUBE_API_KEY` no `.env`.
- [X] Em `YoutubeMusicService`, implementar `searchCandidates(Track sourceTrack)`:
  - [X] Montar a query como `sourceTrack.artist() + " " + sourceTrack.title()`.
  - [X] Chamar `youtube.search().list("snippet")` com `q`, `type=video`,
        `maxResults` em torno de 5.
  - [X] Mapear cada item do resultado para um `Track` (usando o `videoId` como
        `id` e o `title` retornado pelo YouTube).
- [X] Testar isoladamente: para 2-3 músicas conhecidas, imprimir os 5
      candidatos brutos retornados (sem nenhum matching ainda) e olhar se fazem
      sentido.

---

## Fase 4 — Matching visual

- [X] Escolher uma lib de similaridade de string (ex: adicionar
      `info.debatty:java-string-similarity` no `../pom.xml`) ou implementar
  Levenshtein à mão como exercício.
- [X] Em `TrackMatcher.findBestMatch(...)`:
  - [X] Normalizar título/artista: minúsculo, remover acentos, remover trechos
        entre parênteses/colchetes (`(Official Video)`, `[Lyrics]`, etc.).
  - [X] Calcular o score de similaridade entre o título normalizado da origem
        e o de cada candidato.
  - [X] Aplicar penalidade/descarte se o candidato tiver palavras como `cover`,
        `live`, `remix`, `8d audio` e a faixa original não tiver.
  - [X] Retornar o candidato de maior score como `MatchResult`.
- [X] Atualizar `PlaylistConverterApplication.run()` para, em vez de uma faixa
      só, iterar por todas as faixas da playlist de teste:
  - [X] Para cada uma: buscar candidatos no YouTube, rodar o `TrackMatcher`,
        imprimir `origem -> candidato encontrado -> confiança`.
- [X] Rodar contra uma playlist real (ex: a do DJ) e revisar visualmente os
      resultados: quantos acertaram, quantos "viajaram".
- [X] Ajustar as regras de normalização/penalidade com base nos erros
      observados, repetindo até a taxa de acerto parecer boa o suficiente.

*(Pare aqui até o matching estar confiável — só depois disso vale seguir para
a Fase 5.)*

---

## Fase 5 — Persistência (SQLite)

- [X] Criar classe `SqliteSyncStateStore` implementando `SyncStateStore`.
- [X] Criar método de inicialização que roda os `CREATE TABLE IF NOT EXISTS`
      (`sync_job`, `sync_run`, `synced_track`) na primeira execução.
- [X] Definir onde o arquivo `.db` fica salvo (sugestão:
      `System.getProperty("user.home") + "/.playlist-converter/data.db"`).
- [X] Implementar `isAlreadySynced`, `markSynced`, `getHistory` com
      `PreparedStatement`.
- [] Testar manualmente: rodar duas vezes seguidas e confirmar que a segunda
      execução reconhece as faixas já sincronizadas.

---

## Fase 6 — Conversão real (escrita)

- [ ] Estender a autenticação Spotify para incluir escopo de escrita
      (`playlist-modify-private` ou `playlist-modify-public`).
- [ ] Implementar autenticação OAuth do Google (usando `google-oauth-client-jetty`)
      para as operações de escrita no YouTube.
- [ ] Implementar `createPlaylist` e `addTracks` em `SpotifyMusicService` e
      `YoutubeMusicService`.
- [ ] Criar a classe `PlaylistConverter` (orquestrador) que:
  - [ ] Lê as faixas de origem.
  - [ ] Para cada uma, verifica `isAlreadySynced`; se sim, pula.
  - [ ] Se não, busca candidato + matching; se confiança acima de um limiar,
        adiciona à playlist de destino e grava em `markSynced`.
  - [ ] Ao final, imprime um resumo (adicionadas / puladas / não encontradas).
- [ ] Rodar o fluxo completo ponta a ponta em uma playlist de teste pequena
      (5-10 músicas) antes de usar numa playlist grande.

---

## Fase 7 — Conversão inversa

- [X] Implementar `getPlaylistTracks` em `YoutubeMusicService` (ler playlist do
      YouTube via `playlistItems.list`).
- [ ] Implementar `searchCandidates` em `SpotifyMusicService` (usando o
      endpoint de busca do Spotify).
- [ ] Validar que o mesmo `PlaylistConverter` da Fase 6 funciona sem alteração,
      só invertendo qual `MusicService` é `source` e qual é `destination`.

---

## Fase 8 — Interface web

**Pré-requisito: Fase 6 completa.** A interface é só uma porta de entrada nova
para o `PlaylistConverter` que já vai existir — sem o orquestrador funcionando
via código, não tem o que a tela chamar.

- [ ] Adicionar a dependência `spring-boot-starter-web` no `../pom.xml` (sobe um
      servidor Tomcat embutido junto com a aplicação, sem precisar de outro
      processo separado).
- [ ] Criar um enum `ConversionDirection` com `SPOTIFY_TO_YOUTUBE` e
      `YOUTUBE_TO_SPOTIFY`, para representar a escolha do usuário de forma
      explícita (em vez de duas strings soltas).
- [ ] Criar um DTO `ConversionRequest` (record) com os campos que a tela vai
      enviar: `direction` (o enum acima), `sourcePlaylistLink` (aceita link
      completo ou só o ID) e `destinationPlaylistName` (nome da playlist nova
      a ser criada no destino).
- [ ] Criar um método utilitário `extractPlaylistId(String linkOuId)` que
      reconhece se o usuário colou uma URL completa (Spotify ou YouTube) ou só
      o ID puro, e devolve sempre o ID — assim a tela não exige que o usuário
      saiba extrair o ID manualmente (como você tem feito até agora).
- [ ] Criar `PlaylistConverterController` (`@RestController`) com um endpoint
      `POST /convert`:
  - [ ] Recebe um `ConversionRequest` no corpo da requisição.
  - [ ] Com base em `direction`, decide qual `MusicService` é origem e qual é
        destino (reaproveitando a mesma lógica bidirecional da Fase 7).
  - [ ] Chama o `PlaylistConverter.convert(...)` já existente.
  - [ ] Devolve um `ConversionResponse` (record) com o resumo: quantas faixas
        foram adicionadas, puladas (duplicadas) e não encontradas.
- [ ] Criar a página em `src/main/resources/static/index.html`:
  - [ ] Um `<select>` com as duas opções de direção (Spotify → YouTube Music /
        YouTube Music → Spotify).
  - [ ] Um campo de texto para colar o link (ou ID) da playlist de origem.
  - [ ] Um campo de texto para o nome da playlist de destino.
  - [ ] Um botão "Converter".
  - [ ] Uma área que mostra o resultado (via JavaScript puro com `fetch()`
        chamando `POST /convert`), incluindo estado de "carregando..." enquanto
        a conversão roda e uma mensagem de erro clara se algo falhar.
- [ ] Rodar `mvn spring-boot:run` e testar pelo navegador em
      `http://localhost:8080`.
- [ ] Validar o fluxo completo pela tela, do início ao fim, com uma playlist
      pequena de teste.
- [ ] **Ponto de atenção a resolver quando chegar aqui:** o login OAuth hoje
      está pensado para CLI (abre navegador + sobe servidor local temporário
      só para capturar o retorno). Numa página web isso precisa ser adaptado —
      vale conversar sobre as opções nesse momento, antes de implementar.

---

## Backlog (depois do MVP funcionando)

- [ ] Notificação via webhook (Discord/Slack) ao final de cada execução.
- [ ] Paralelizar as buscas de matching com `ExecutorService`/`CompletableFuture`.
- [ ] Revisão de matches de baixa confiança antes de confirmar a conversão
      (mostrar na interface web os casos duvidosos para o usuário decidir).
