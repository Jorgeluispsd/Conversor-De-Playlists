package com.jorge.playlistconverter.youtube.authorization;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.json.gson.GsonFactory;
import com.jorge.playlistconverter.errors.youtube.GoogleClientSecretsException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

public class GoogleClientSecretsLoader {

    private final Path credentialsPath;

    public GoogleClientSecretsLoader(){
        this(Path.of(System.getProperty("user.home"),
                ".playlist-converter", "google-client-secret.json"));
    }

    public GoogleClientSecretsLoader(Path credentialsPath) {
        this.credentialsPath = Objects.requireNonNull(credentialsPath,
                "O caminho das credenciais Google não pode ser nulo.");
    }

    public GoogleClientSecrets load(){
        String content = readFile();

        GoogleClientSecrets secrets;

        try(StringReader reader = new StringReader(content)){
            secrets = GoogleClientSecrets.load(
                    GsonFactory.getDefaultInstance(), reader);

        }catch (IOException | RuntimeException e){
            throw new GoogleClientSecretsException(
                    "O arquivo de credenciais Google contém um JSON inválido.");
        }

        validate(secrets);

        return secrets;
    }

    private String readFile(){
        try {
            return Files.readString(credentialsPath, StandardCharsets.UTF_8);
        }catch (NoSuchFileException e){
            throw new GoogleClientSecretsException("Arquivo de credenciais Google não encontrado. "
                    + "Configure google-client-secret.json "
                    + "na pasta .playlist-converter.", e);

        }catch (IOException e){
            throw new GoogleClientSecretsException(
                    "Não foi possível ler o arquivo de credenciais Google.", e);
        }
    }

    private void validate(GoogleClientSecrets secrets){
        if (secrets == null || secrets.getInstalled() == null || secrets.getWeb() != null){
            throw new GoogleClientSecretsException(
                    "Use credenciais OAuth do tipo Aplicativo para computador.");
        }

        GoogleClientSecrets.Details details = secrets.getInstalled();

        if (isBlank(details.getClientId())
                    || isBlank(details.getClientSecret())
                    || isBlank(details.getAuthUri())
                    || isBlank(details.getTokenUri())){

            throw new GoogleClientSecretsException(
                    "As credenciais Google estão incompletas. "
                            + "Baixe novamente o JSON do cliente OAuth.");
        }
    }

    private boolean isBlank(String value){
        return value == null || value.isBlank();
    }
}
