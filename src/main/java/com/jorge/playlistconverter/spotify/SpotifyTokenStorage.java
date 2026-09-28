package com.jorge.playlistconverter.spotify;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Optional;

@Slf4j
public class SpotifyTokenStorage {

    public record StoredToken(String refreshToken, String scope) {}

    private static final String TOKENS_FILE = "spotify_tokens.json";

    private final Path tokensFilePath;
    private final Gson gson = new Gson();

    public SpotifyTokenStorage(){
        Path appDir = Paths.get(System.getProperty("user.home"), ".playlist-converter");
        this.tokensFilePath = appDir.resolve(TOKENS_FILE);

        try {
            Files.createDirectories(appDir);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar diretório de configuração", e);
        }
    }

    public void save(String refreshToken, String scope){
        JsonObject data = new JsonObject();
        data.addProperty("refresh_token", refreshToken);
        data.addProperty("scope", scope);
        data.addProperty("save_at", Instant.now().toString());

        try {
            Files.writeString(tokensFilePath, gson.toJson(data), StandardCharsets.UTF_8);
            restrictPermissions();
            log.info("Tokens salvo com sucesso na pasta");
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar tokens", e);
        }
    }

    public Optional<StoredToken> load(){
        if (!Files.exists(tokensFilePath)){
            return Optional.empty();
        }

        try {
            String json = Files.readString(tokensFilePath, StandardCharsets.UTF_8);
            JsonObject data = gson.fromJson(json, JsonObject.class);

            return Optional.of(new StoredToken(
                    data.get("refresh_token").getAsString(),
                    data.get("scope").getAsString()));

        }catch (Exception e){
            log.warn("Arquivo de tokens ilegível ou em formato antigo; Será necessário novo login");
            return Optional.empty();
        }
    }

    public void delete(){
        try {
            if (Files.deleteIfExists(tokensFilePath)){
                log.info("Refresh token salvo foi removido");
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao remover o arquivo de tokens", e);
        }
    }

    private void restrictPermissions(){
        try {
            Files.setPosixFilePermissions(tokensFilePath, PosixFilePermissions.fromString("rw-------"));
        }catch (UnsupportedOperationException | IOException e){
            log.warn("Permissões POSIX indisponíveis neste sistema");
        }
    }
}
