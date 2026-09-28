package com.jorge.playlistconverter.spotify;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;

import java.io.IOException;

@Slf4j
public class SpotifyClientCredentialsService {

    private final SpotifyApi spotifyApi;

    public SpotifyClientCredentialsService(SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
    }

    public void ensureAuthenticated() throws IOException, SpotifyWebApiException, ParseException {
        if (spotifyApi.getAccessToken() == null || spotifyApi.getAccessToken().isBlank()){
            log.info("Obtendo token de acesso via Client Credentials Flow");
            authenticate();
        }else{
            log.info("Token de acesso já existe");
        }
    }

    private void authenticate() throws IOException, SpotifyWebApiException, ParseException{
        ClientCredentials credentials = spotifyApi.clientCredentials()
                .build()
                .execute();

        spotifyApi.setAccessToken(credentials.getAccessToken());
        log.info("Token de acesso obtido com sucesso.");
    }

    public SpotifyApi getSpotifyApi(){
        return spotifyApi;
    }
}
