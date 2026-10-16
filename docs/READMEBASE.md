# Base técnica — Conversor de Playlists

O README da raiz apresenta o projeto e seu progresso; [PROJECT_LIMITATIONS.md](PROJECT_LIMITATIONS.md) registra decisões de escopo. O planejamento detalhado é mantido localmente.

## Implementado

- Java 21 e Spring Boot com injeção via classes de configuração.
- Spotify: Authorization Code + PKCE, callback local, armazenamento de refresh token, restauração da sessão e renovação. SpotifyMusicService usa esse fluxo para leitura; classes/beans de Client Credentials ainda existem, mas não são o fluxo de leitura desse serviço.
- YouTube: leitura de playlist e busca de até cinco candidatos via chave de API. Escrita autenticada ainda não implementada.
- Modelo comum Song e MatchResult; TrackMatcher com Jaro-Winkler do Apache Commons Text. Pontuação representa semelhança, não garantia de identidade do áudio.
- H2: schema e consultas em src/main/resources/sql; preparação em H2DatabaseInitializer, operações em H2SyncStateStore e leitura dos recursos em SqlResourceLoader.
- Exceptions por responsabilidade; causa preservada, interrupção sinalizada, retry único Spotify após 401, quotaExceeded YouTube e 429 Spotify específicos.

## Execução local

Copie .env.example para .env e configure suas credenciais. O fluxo PKCE usa SPOTIFY_CLIENT_ID e SPOTIFY_REDIRECT_URI; SPOTIFY_CLIENT_SECRET permanece para os componentes de Client Credentials existentes. YOUTUBE_API_KEY permite as consultas atuais ao YouTube. Registre o redirect http://127.0.0.1:8888/callback no app Spotify.

Execute PlaylistConverterApplication no IntelliJ, habilitando o runner manual desejado. O main desabilita headless para permitir abertura do navegador no computador local. O fallback manual permanece quando BROWSE não está disponível.

Também é possível executar:

```text
mvn spring-boot:run
```

Isso inicia a aplicação e os runners habilitados; não executa uma conversão completa ainda.

## Persistência e console

A pasta ~/.playlist-converter guarda spotify_tokens.json e o banco H2, cujo caminho base é data.db. O console local imprime seu link nos logs. Use a URL JDBC jdbc:h2:~/.playlist-converter/data.db e as credenciais do banco criado. As tabelas são sync_job, sync_run e synced_track. Histórico de sincronização não é cache de pesquisas.

## Testes

```text
mvn test
mvn test -Dtest=YoutubeMusicServiceTest
```

Sem Maven no PATH, execute pelo Run Anything do IntelliJ (Ctrl duas vezes). O primeiro comando executa a suíte; o segundo seleciona uma classe. Atualmente são 36 testes aprovados: 19 matcher, 4 SpotifyMusicService, 4 callback, 3 H2 e 6 YouTube. Mocks não consomem quota; o H2 de teste usa uma pasta temporária.

## Tratamento atual dos limites

Quota YouTube e 429 Spotify encerram a operação com exceptions específicas, preservando a causa. Spotify guarda Retry-After quando positivo, mas não espera nem repete automaticamente. A biblioteca atual não disponibiliza o motivo estruturado QUOTA_EXCEEDED. A política de retomada, tentativas limitadas e recuperação da ordem pertence à Fase 8.

## Próximas fases

- Fase 6: escrita Spotify → YouTube, orquestrador e histórico básico. OAuth Google e criação de playlist YouTube já validados; inserção pendente. Validar primeiro com playlist pequena.
- Fase 7: busca de candidatos Spotify e conversão inversa. Leitura YouTube já implementada.
- Fase 8: progresso persistido, retomada, tentativas limitadas e recuperação da ordem original.
- Fase 9: interface web com análise, prévia comparativa, escolhas, cancelamento e confirmação antes da escrita.

Erros adicionais e comentários antigos serão revisados conforme cada bloco avançar. Notificações e paralelismo continuam no backlog.
