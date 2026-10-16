# 🎵 Conversor de Playlists

Ferramenta em Java para migrar playlists entre **Spotify** e **YouTube Music**, nos dois sentidos, evitando duplicar músicas em conversões repetidas.

## 💡 Motivação

Este projeto nasceu de uma necessidade pessoal: em diversas ocasiões, como eventos ou encontros com amigos, eu ouvia músicas incríveis que me marcavam, mas por não ter Spotify na época, ficava limitado a apenas ouvi-las naquele momento. A solução ideal seria ter as mesmas playlists em diferentes aplicativos de música, sem o trabalho tedioso de pesquisar cada música manualmente.

O objetivo é permitir uma conversão direta e eficiente de playlists entre Spotify e YouTube Music, eliminando a necessidade de reconstruir playlists do zero quando você decide mudar de plataforma de streaming ou simplesmente quer ter suas playlists favoritas disponíveis em mais de um serviço.

## 🚧 Status do projeto

Em desenvolvimento — construído em fases, cada uma validada antes de avançar para a próxima. As funcionalidades implementadas e os próximos passos estão resumidos abaixo.

- [x] Setup do projeto (Maven + Spring Boot)
- [x] Autenticação com Spotify (OAuth + PKCE + persistência de refresh_token)
- [x] Leitura de playlists do Spotify
- [x] Busca de músicas equivalentes no YouTube
- [x] Matching e cálculo de confiança entre faixas
- [x] Persistência do histórico (H2) e prevenção de duplicatas
- [x] Leitura de playlists do YouTube
- [x] Tratamento específico de quota YouTube e de HTTP 429 Spotify
- [x] 36 testes automatizados aprovados (13/10/2026)
- [ ] Conversão real (Spotify → YouTube Music)
- [ ] Conversão inversa (YouTube Music → Spotify)
- [ ] Interface web com prévia, revisão e confirmação (Fase 9)

## ⚙️ Como funciona

A arquitetura é pensada para suportar as duas direções de conversão: `SpotifyMusicService` e `YoutubeMusicService` implementam a interface `MusicService`. Leitura, busca no YouTube, matching e persistência já existem como componentes. OAuth Google e criação de playlists YouTube com visibilidade configurável já foram validados. Inserção de faixas e o orquestrador `PlaylistConverter` seguem pendentes na Fase 6; retomada e recuperação da ordem serão detalhadas na Fase 8.

```
model/       -> Song, MatchResult — estruturas de dados comuns aos dois serviços
service/     -> MusicService — contrato comum entre Spotify e YouTube
spotify/     -> implementação do lado Spotify
youtube/     -> implementação do lado YouTube
matcher/     -> lógica de similaridade para encontrar a música equivalente
state/       -> histórico de sincronizações e controle de duplicata (H2)
state/database/ -> H2DatabaseInitializer e H2SyncStateStore
state/sql/   -> SqlResourceLoader
errors/      -> exceções por autorização, Spotify, YouTube e persistência
config/      -> configuração dos clientes de API (injeção de dependência via Spring)
```

O matcher compara os candidatos e aplica penalidades para versões diferentes. Sua pontuação indica semelhança, sem garantir que o áudio seja o mesmo. A revisão e confirmação pelo usuário estão planejadas para a Fase 9.

## 🛠️ Tecnologias

- Java 21 + Spring Boot
- [spotify-web-api-java](https://github.com/spotify-web-api-java/spotify-web-api-java) — cliente para a Spotify Web API
- [google-api-services-youtube](https://developers.google.com/youtube/v3) — cliente oficial para a YouTube Data API v3
- H2 Database — histórico local de sincronizações e prevenção de duplicatas
- Maven

## 🚀 Como rodar localmente

### Pré-requisitos

- Java 21+
- Maven
- Conta Spotify **Premium** (exigido pelo Development Mode da Spotify para acesso à API)
- Conta Google (para gerar uma chave da YouTube Data API v3)

### Setup

1. Clone o repositório:
   ```
   git clone https://github.com/seu-usuario/conversor-de-playlists.git
   cd conversor-de-playlists
   ```

2. Crie um app em [developer.spotify.com/dashboard](https://developer.spotify.com/dashboard) e, em *Users Management*, adicione o e-mail da conta Spotify que você vai usar para testar (obrigatório em Development Mode — limite de 5 usuários autorizados por app).

3. Crie um projeto no [Google Cloud Console](https://console.cloud.google.com/), ative a **YouTube Data API v3** e gere uma chave de API.

4. Copie `.env.example` para `.env` e preencha com suas credenciais:
   ```
   SPOTIFY_CLIENT_ID=
   SPOTIFY_CLIENT_SECRET=
   SPOTIFY_REDIRECT_URI=http://127.0.0.1:8888/callback
   YOUTUBE_API_KEY=
   ```

5. Rode a aplicação pelo IntelliJ usando `PlaylistConverterApplication`, ou pelo Maven:
   ```
   mvn spring-boot:run
   ```

Os `CommandLineRunner` atuais são laboratórios manuais: habilite apenas o cenário que deseja validar. Executar a aplicação ainda não realiza uma conversão completa. O `main` configura `headless=false` para permitir abertura do navegador no ambiente local; a sessão Spotify salva é reutilizada nas execuções seguintes.

### Testes e banco local

Execute `mvn test` para rodar os 36 testes. Se `mvn` não estiver no PATH, use o Maven do IntelliJ: pressione Ctrl duas vezes e execute `mvn test` pelo Run Anything.

Os testes usam mocks e um banco H2 temporário, sem chamadas reais às APIs nem acesso ao banco pessoal. A suíte inclui 19 testes do matcher, 4 do serviço Spotify, 4 do callback, 3 da persistência H2 e 6 do serviço YouTube.

Os dados locais ficam em `~/.playlist-converter/`: `spotify_tokens.json` guarda a sessão e o banco usa o caminho base `data.db` (o H2 acrescenta sua extensão ao arquivo). O console H2 informa seu link nos logs da aplicação. Use a URL JDBC `jdbc:h2:~/.playlist-converter/data.db` e as mesmas credenciais usadas na criação do banco. Mantenha a aplicação rodando e execute, por exemplo:

```sql
SELECT * FROM sync_job;
SELECT * FROM sync_run;
SELECT * FROM synced_track;
```

> ⚠️ **Nenhuma credencial deste projeto é distribuída publicamente.** Cada pessoa que for rodar o projeto precisa criar seu próprio app no Spotify e sua própria chave no Google Cloud — é assim que projetos open source que integram com APIs de terceiros funcionam. O `.env` nunca é commitado (veja `.gitignore`).

## 📌 Limitações conhecidas

### Acesso e quotas

O acesso às playlists depende das permissões da conta, dos escopos e das regras vigentes do provedor. O serviço informa acesso negado quando recebe HTTP 403; esse código sozinho não determina a causa. Salvar uma playlist na biblioteca não deve ser apresentado como garantia de acesso pela API.

O projeto trabalha com as quotas padrão; solicitar aumento está fora do escopo atual. Uma playlist inédita com 280 músicas pode exigir 280 pesquisas no YouTube e três janelas de quota padrão. Os detalhes e decisões estão em [docs/PROJECT_LIMITATIONS.md](docs/PROJECT_LIMITATIONS.md).

Quando recebe `quotaExceeded`, o YouTube lança uma exceção específica. No Spotify, HTTP 429 preserva o prazo `Retry-After` quando positivo. A biblioteca atual não preserva o motivo estruturado `QUOTA_EXCEEDED`, portanto o projeto não consegue distinguir os dois tipos de limitação com segurança. Os serviços encerram a operação sem retornar uma lista parcial como resultado completo; espera automática e retomada serão detalhadas na Fase 8.

### Outras limitações

- O Spotify limita apps em Development Mode a 5 usuários autorizados e exige conta Premium para desenvolvimento — isso afeta quem consegue testar o projeto sem passar pelo processo de extensão de quota da Spotify.
- O matching entre faixas é baseado em similaridade de texto (título/artista); casos como remixes, covers ou versões ao vivo podem exigir revisão manual quando a confiança do match for baixa.

## 📄 Licença

Este projeto está sob a licença MIT — veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

Desenvolvido por [Jorge] como projeto de portfólio, aplicando integração com APIs REST, OAuth, persistência de dados e arquitetura orientada a interfaces em Java.
