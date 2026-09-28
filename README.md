# 🎵 Conversor de Playlists

Ferramenta em Java para migrar playlists entre **Spotify** e **YouTube Music**, nos dois sentidos, evitando duplicar músicas em conversões repetidas.

## 💡 Motivação

Ferramentas prontas como TuneMyMusic e Soundiiz resolvem esse problema, mas não sincronizam automaticamente novas músicas adicionadas a uma playlist depois da primeira conversão — cada atualização precisa ser refeita manualmente. Este projeto nasceu dessa necessidade: converter playlists entre os dois serviços de forma incremental, adicionando apenas as músicas novas em conversões futuras.

## 🚧 Status do projeto

Em desenvolvimento — construído em fases, cada uma validada antes de avançar para a próxima. Confira o progresso detalhado em [`docs/TASKS.md`](docs/TASKS.md).

- [x] Setup do projeto (Maven + Spring Boot)
- [x] Autenticação com Spotify (OAuth + PKCE + persistência de refresh_token)
- [ ] Leitura de playlists do Spotify
- [x] Busca de músicas equivalentes no YouTube
- [x] Matching e cálculo de confiança entre faixas
- [x] Persistência do histórico (SQLite) e prevenção de duplicatas
- [ ] Conversão real (Spotify → YouTube Music)
- [ ] Conversão inversa (YouTube Music → Spotify)

## ⚙️ Como funciona

A arquitetura é pensada para suportar as duas direções de conversão sem duplicar lógica: `SpotifyMusicService` e `YoutubeMusicService` implementam a mesma interface `MusicService`, e um orquestrador central lida com a leitura da origem, busca de equivalência, matching e escrita no destino — indiferente de qual serviço é origem e qual é destino.

```
model/       -> Track, MatchResult — estruturas de dados comuns aos dois serviços
service/     -> MusicService — contrato comum entre Spotify e YouTube
spotify/     -> implementação do lado Spotify
youtube/     -> implementação do lado YouTube
matcher/     -> lógica de similaridade para encontrar a música equivalente
state/       -> histórico de sincronizações e controle de duplicata (SQLite)
config/      -> configuração dos clientes de API (injeção de dependência via Spring)
```

Antes de qualquer música ser adicionada de fato, cada faixa passa por um cálculo de confiança de match — evitando trocar uma faixa original por um cover, remix ou versão ao vivo por engano.

## 🛠️ Tecnologias

- Java 17 + Spring Boot
- [spotify-web-api-java](https://github.com/spotify-web-api-java/spotify-web-api-java) — cliente para a Spotify Web API
- [google-api-services-youtube](https://developers.google.com/youtube/v3) — cliente oficial para a YouTube Data API v3
- SQLite (via `sqlite-jdbc`) — histórico local de sincronizações
- Maven

## 🚀 Como rodar localmente

### Pré-requisitos

- Java 17+
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

5. Rode a aplicação:
   ```
   mvn spring-boot:run
   ```

> ⚠️ **Nenhuma credencial deste projeto é distribuída publicamente.** Cada pessoa que for rodar o projeto precisa criar seu próprio app no Spotify e sua própria chave no Google Cloud — é assim que projetos open source que integram com APIs de terceiros funcionam. O `.env` nunca é commitado (veja `.gitignore`).

## 📌 Limitações conhecidas

- O Spotify limita apps em Development Mode a 5 usuários autorizados e exige conta Premium para desenvolvimento — isso afeta quem consegue testar o projeto sem passar pelo processo de extensão de quota da Spotify.
- O matching entre faixas é baseado em similaridade de texto (título/artista); casos como remixes, covers ou versões ao vivo podem exigir revisão manual quando a confiança do match for baixa.

## 📄 Licença

Este projeto está sob a licença MIT — veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

Desenvolvido por [Jorge] como projeto de portfólio, aplicando integração com APIs REST, OAuth, persistência de dados e arquitetura orientada a interfaces em Java.
