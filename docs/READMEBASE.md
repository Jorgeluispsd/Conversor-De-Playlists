# Conversor de Playlists

Conversor bidirecional de playlists entre Spotify e YouTube Music, em Java + Spring Boot.

## Setup inicial

1. Copie `.env.example` para `.env` e preencha com suas próprias credenciais:
   - **Spotify**: crie um app em https://developer.spotify.com/dashboard e adicione
     seu e-mail em *Users Management* (obrigatório em Development Mode — limite de
     5 usuários autorizados, e a conta usada precisa ser Premium).
   - **YouTube**: crie um projeto no Google Cloud Console, ative a *YouTube Data API v3*
     e gere uma chave de API.
2. Confirme que `.env` está no `../.gitignore` antes do primeiro commit.
3. Importe o projeto no seu IDE (Maven) e rode `PlaylistConverterApplication`.

## Estrutura do projeto

```
model/       -> Track, MatchResult (comuns aos dois serviços)
service/     -> interface MusicService (contrato comum Spotify/YouTube)
spotify/     -> SpotifyMusicService (implementação do lado Spotify)
youtube/     -> YoutubeMusicService (implementação do lado YouTube)
matcher/     -> TrackMatcher (lógica de similaridade / matching)
state/       -> SyncStateStore (histórico + controle de duplicata via SQLite)
```

## Roteiro de desenvolvimento

O projeto está organizado em fases, marcadas como comentários `TODO (Fase N)`
dentro do código. Siga nesta ordem:

1. **Fase 1** — Autenticação Spotify (Authorization Code + PKCE, leitura)
2. **Fase 2** — Listar as faixas de uma playlist do Spotify
3. **Fase 3** — Buscar candidatos equivalentes no YouTube (`search.list`)
4. **Fase 4** — Matching: calcular similaridade e **imprimir no console** o
   resultado (faixa original vs. candidato encontrado + confiança), sem gravar
   nada ainda. *Você está começando por aqui.*
5. **Fase 5** — Persistência: tabelas SQLite (`sync_job`, `sync_run`, `synced_track`)
6. **Fase 6** — Escrita real: criar playlist de destino, checar duplicata, adicionar faixas
7. **Fase 7** — Conversão inversa (YouTube → Spotify), reaproveitando o mesmo `MusicService`

Notificações (webhook) e paralelismo ficam para depois que o fluxo básico
estiver funcionando ponta a ponta.

## Rodando

```
mvn spring-boot:run
```

Por enquanto o `CommandLineRunner` em `PlaylistConverterApplication` só confirma
que as variáveis de ambiente foram carregadas e roda um matching de exemplo com
dados fictícios — substitua pelo fluxo real conforme for avançando pelas fases.
