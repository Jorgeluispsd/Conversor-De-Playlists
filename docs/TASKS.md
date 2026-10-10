# Tasks — Conversor de Playlists

Checklist de desenvolvimento, na ordem recomendada. Marque cada item conforme for
concluindo. As referências de arquivo/classe já existem no esqueleto do projeto.

Limitações dos provedores e decisões de escopo: [PROJECT_LIMITATIONS.md](PROJECT_LIMITATIONS.md).

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

- [X] Criar app em https://developer.spotify.com/dashboard.
- [X] Em *Users Management*, adicionar seu próprio e-mail Spotify como usuário
      autorizado (obrigatório em Development Mode).
- [X] Copiar Client ID e Client Secret para o `.env`.
- [X] Criar classe `com.jorge.playlistconverter.spotify.SpotifyAuthService`
      responsável por:
  - [X] Montar a URL de autorização (Authorization Code + PKCE, escopo
        `playlist-read-private`).
  - [X] Implementar abertura da URL com fallback manual; diagnóstico do suporte no ambiente atual ainda pendente.
  - [X] Subir um servidor HTTP local simples (ex: `com.sun.net.httpserver.HttpServer`)
        só para capturar o `code` que o Spotify devolve no redirect.
  - [X] Trocar o `code` por um `access_token` + `refresh_token` via
        `spotifyApi.authorizationCodePKCERequest(...)`.
- [X] Testar manualmente: rodar o fluxo e confirmar no console que recebeu um
      access token (não precisa usá-lo ainda).
- [X] Implementar tratamento para `access_token` expirado (curto prazo, ~1 hora):
      detectar a resposta de erro do Spotify e renovar automaticamente usando
      o `refresh_token` salvo, sem exigir novo login do usuário.
- [X] Implementar tratamento para `refresh_token` expirado (`invalid_grant`,
      acontece 6 meses após a autorização original, mudança da Spotify de
      2026): quando esse erro específico vier, descartar o token salvo e
      disparar o fluxo de login novamente (reabrir o navegador), em vez de
      tentar renovar de novo — uma nova tentativa de refresh vai falhar do
      mesmo jeito.

---

## Fase 2 — Listar faixas de uma playlist do Spotify

Estado atual: leitura autenticada por Authorization Code + PKCE via SpotifyAuthService. A abordagem híbrida com Client Credentials foi substituída e não representa o código atual.

- [X] Renomear o modelo comum Track para Song.
- [X] Injetar SpotifyAuthService no SpotifyMusicService e usar seu SpotifyApi.
- [X] Implementar getPlaylistTracks com getPlaylistItems, paginação de 50 itens e mapeamento para Song.
- [X] Preservar a ordem da origem e mapear título, artista principal e duração.
- [X] Invalidar o access token após 401, restabelecer autenticação e repetir a requisição apenas uma vez.
- [X] Validar leitura de playlist e reutilização de sessão pelo runner.

---
## Fase 3 — Buscar candidatos no YouTube

- [X] Criar projeto no Google Cloud Console, ativar a **YouTube Data API v3**,
      gerar uma **API key** (não precisa de OAuth para busca).
- [X] Copiar a chave para `YOUTUBE_API_KEY` no `.env`.
- [X] Em `YoutubeMusicService`, implementar `searchCandidates(Song sourceSong)`:
  - [X] Montar a query como `sourceSong.artist() + " " + sourceSong.title()`.
  - [X] Chamar `youtube.search().list("snippet")` com `q`, `type=video`,
        `maxResults` em torno de 5.
  - [X] Mapear cada item do resultado para um `Song` (usando o `videoId` como
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

## Fase 5 — Persistência (H2)

- [X] Criar classe `H2SyncStateStore` implementando `SyncStateStore`.
- [X] Criar método de inicialização que roda os `CREATE TABLE IF NOT EXISTS`
      (`sync_job`, `sync_run`, `synced_track`) na primeira execução.
- [X] Definir onde o arquivo `.db` fica salvo (sugestão:
      `System.getProperty("user.home") + "/.playlist-converter/data.db"`).
- [X] Implementar `isAlreadySynced`, `markSynced`, `getHistory` com
      `PreparedStatement`.
- [X] Testar manualmente: rodar duas vezes seguidas e confirmar que a segunda
      execução reconhece as faixas já sincronizadas.

---

## Refatoração e validação antes da Fase 6 — atualizado em 10/10/2026

- [X] Separar callback, sessão, resultado e status no package spotify.callback.
- [X] Extrair geração da autorização PKCE e injetar os componentes de autenticação.
- [X] Injetar SpotifyTokenStorage e limitar getters do SpotifyAuthService.
- [X] Diferenciar token ausente, conteúdo inválido e falha técnica de armazenamento.
- [X] Especificar erros de timeout, rejeição, callback, abertura do servidor e autenticação.
- [X] Especificar erros de leitura Spotify, preservando causas, interrupção e retry único após 401.
- [X] Especificar erros de leitura e busca YouTube, distinguindo respostas estruturadas da API de outras IOExceptions.
- [X] Extrair SqlResourceLoader e disponibilizar link do console H2 local na inicialização.
- [X] Criar dois testes de interrupção do SpotifyMusicService; manter os 19 testes do matcher.
- [X] Configurar agente Mockito no Maven e atualizar Logback para 1.6.5.
- [X] Validar testes Maven e execução normal; busca e leitura YouTube testadas manualmente por Jorge.
- [ ] Tratar erros de leitura de recursos SQL e persistência H2, preservando duplicatas e regras do histórico.
- [ ] Extrair preparação do banco em bloco separado.
- [ ] Concluir revisão dos tratamentos restantes de callback/autenticação e investigar fallback do navegador.
- [ ] Criar testes isolados pertinentes para os cenários de falha ainda não exercitados.
- [ ] Tratar limites das APIs: motivo quotaExceeded no YouTube; distinguir rate limit e quota do Spotify, respeitar Retry-After quando aplicável e limitar tentativas.
- [ ] Validar esses limites com respostas simuladas, sem consumir quota real.
- [ ] Concluir atualização de README, READMEBASE, comentários e TODOs.

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
- [ ] Preservar progresso para retomar após quota esgotada ou falha, sem repetir inserções concluídas; distinguir resultados de pesquisa de histórico de sincronização.
- [ ] Avaliar obtenção e reutilização de candidatos dentro das políticas dos provedores, sem prometer cobertura de playlists grandes com a quota padrão.

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
- [ ] Criar `PlaylistConverterController` (`@RestController`) com operações separadas de análise e execução confirmada (contratos a definir):
  - [ ] Recebe um `ConversionRequest` no corpo da requisição.
  - [ ] Com base em `direction`, decide qual `MusicService` é origem e qual é
        destino (reaproveitando a mesma lógica bidirecional da Fase 7).
  - [ ] Adaptar o orquestrador da Fase 6 para separar obtenção/comparação de candidatos da escrita no destino.
  - [ ] Retornar a prévia sem criar a playlist; executar a criação apenas após confirmação explícita, usando os itens escolhidos.
  - [ ] Devolve um `ConversionResponse` (record) com o resumo: quantas faixas
        foram adicionadas, puladas (duplicadas) e não encontradas.
- [ ] Implementar o fluxo de prévia, revisão e confirmação na Fase 8:
  - [ ] Mostrar as faixas originais na ordem da playlist ao lado dos candidatos, com links, pontuação e motivos da comparação.
  - [ ] Distinguir correspondência forte, duvidosa, nenhum candidato adequado e busca não concluída por quota ou falha técnica.
  - [ ] Permitir escolher alternativas já obtidas, excluir itens e solicitar nova pesquisa quando necessário, informando que novas consultas consomem quota.
  - [ ] Permitir confirmar ou cancelar antes de criar a playlist; incluir apenas os itens selecionados.
  - [ ] Preservar a prévia e as escolhas para retomada quando aplicável, sem refazer buscas desnecessárias.
  - [ ] Testar que gerar a prévia e cancelar não escrevem no destino, e que confirmar respeita escolhas e ordem.
- [ ] Criar a página em `src/main/resources/static/index.html`:
  - [ ] Um `<select>` com as duas opções de direção (Spotify → YouTube Music /
        YouTube Music → Spotify).
  - [ ] Um campo de texto para colar o link (ou ID) da playlist de origem.
  - [ ] Um campo de texto para o nome da playlist de destino.
  - [ ] Um botão "Analisar", uma tela de revisão e ações "Confirmar conversão" e "Cancelar".
  - [ ] Uma área que mostra o resultado (via JavaScript puro com `fetch()`
        chamando as operações de análise e confirmação), incluindo estado de "carregando..." enquanto
        a conversão roda e uma mensagem de erro clara se algo falhar.
- [ ] Rodar `mvn spring-boot:run` e testar pelo navegador em
      `http://localhost:8080`.
- [ ] Validar o fluxo completo pela tela, do início ao fim, com uma playlist
      pequena de teste.
- [ ] **Ponto de atenção a resolver quando chegar aqui:** o login OAuth hoje
      está pensado para CLI (abre navegador + sobe servidor local temporário
      só para capturar o retorno). Numa página web isso precisa ser adaptado —
      vale conversar sobre as opções nesse momento, antes de implementar.
- [ ] **Multi-tenancy:** A implementação atual é single-user (arquivo local
      de tokens). Para múltiplos usuários, será necessário:
      - Armazenar tokens por usuário (banco de dados em vez de arquivo local)
      - Gerenciar múltiplas sessões simultâneas
      - Adaptar o fluxo OAuth para web (redirect para a aplicação, não localhost)
      - Considerar autenticação de usuários da aplicação (login/senha ou OAuth)

---

## Backlog (depois do MVP funcionando)

- [ ] Notificação via webhook (Discord/Slack) ao final de cada execução.
- [ ] Paralelizar as buscas de matching com `ExecutorService`/`CompletableFuture`.
