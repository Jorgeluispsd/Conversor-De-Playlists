# Revisão e próximos blocos — 08/10/2026

Nota de acompanhamento solicitada pelo Jorge. Não substitui README/TASKS; esses documentos serão atualizados após as alterações e validações.

## Forma de trabalho

- Não editar código diretamente: orientar alterações manuais no IntelliJ.
- Trabalhar em blocos pequenos, explicando o que muda, por quê e como funciona.
- Separar extrações estruturais de mudanças no tratamento de erros.
- Compilar e validar cada bloco antes de continuar; sugerir commits ao concluir.

## Avaliação atual

- SpotifyAuthService: callback, sessão de callback, geração da autorização e armazenamento de tokens já separados/injetados. Manter a coordenação da autenticação no serviço.
- H2SyncStateStore: leitura dos recursos SQL extraída. Extrair também preparação do banco, conforme decisão do Jorge, preservando caminho, schema, ordem de inicialização e regras de persistência.
- SpotifyMusicService/YoutubeMusicService: manter operações, paginação e mapeamento juntos por enquanto; reavaliar quando o fluxo real exigir.
- TrackMatcher: manter algoritmo e testes de caracterização; não introduzir Strategy sem algoritmos alternativos reais.
- PlaylistConverterApplication: laboratório temporário; não investir em separar todos os runners experimentais.

## Ordem de trabalho aprovada

1. Limitar getters de SpotifyAuthService ao acesso atualmente necessário.
2. Revisar erros em blocos: armazenamento de tokens, autenticação/callback, serviços de música e persistência SQL.
3. Criar package errors para exceptions da aplicação quando houver significado claro. Capturas e decisões de recuperação permanecem nas camadas responsáveis; não criar uma classe genérica para capturar tudo.
4. Extrair preparação do banco em bloco estrutural separado das mudanças de erros.
5. Validar os blocos atuais por compilação, testes existentes e testes manuais. Por decisão do Jorge, deixar a ampliação dos testes automatizados para a Fase 6.
6. Atualizar README.md, docs/TASKS.md, docs/READMEBASE.md, comentários e TODOs.
7. Iniciar Fase 6 com conversão real, ampliar testes automatizados e avaliar padrões somente diante do fluxo concreto.

## Pontos concretos a resolver

- SpotifyTokenStorage.load(): separar arquivo ausente, JSON inválido, falha de leitura e erro de programação. Definir explicitamente quais situações permitem novo login e quais devem ser propagadas; preservar arquivo em falhas técnicas.
- SpotifyAuthService: reduzir throws Exception quando adequado, distinguir resultados do login e falhas técnicas, preservar interrupção, timeout, encerramento e sessão em falhas transitórias.
- SpotifyMusicService/YoutubeMusicService: revisar capturas amplas e mensagens, preservar causas e retry único após 401.
- H2SyncStateStore: preservar reconhecimento de SQLState 23505 ao alterar exceptions; preservar regras de conclusão de sync_run e status success de synced_track.
- Lombok @Getter em SpotifyAuthService: getSpotifyApi() é o único getter usado pelo código atual; não expor callback, gerador, armazenamento e estado interno.
- H2ConsoleConfig: listener alterado para ApplicationStartedEvent, informando o link antes dos runners. Remover import não utilizado de ApplicationReadyEvent. Manter acesso local.
- Testes atuais: apenas matcher possui testes automatizados. Selecionar regressões de token storage, PKCE, callback e H2; usar arquivos/bancos temporários, sem tokens pessoais nem chamadas reais às APIs.
- Documentação: corrigir referências a Track, SQLite e fluxo híbrido Client Credentials, distinguindo funcionalidades implementadas de planejadas.

## Validação já relatada

- Login aceito, rejeitado, timeout e reutilização da sessão testados manualmente após extrações de callback e geração da autorização.
- Injeção de SpotifyTokenStorage, leitura SQL e acesso ao console H2 validados manualmente.
- Revisão estática realizada; ela não equivale a execução de todos os testes.
