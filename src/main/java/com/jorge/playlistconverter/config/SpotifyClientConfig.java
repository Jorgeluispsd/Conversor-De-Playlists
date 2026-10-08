package com.jorge.playlistconverter.config;

import com.jorge.playlistconverter.spotify.SpotifyAuthService;
import com.jorge.playlistconverter.spotify.SpotifyClientCredentialsService;
import com.jorge.playlistconverter.spotify.SpotifyMusicService;
import com.jorge.playlistconverter.spotify.authorization.SpotifyAuthorizationGenerator;
import com.jorge.playlistconverter.spotify.callback.SpotifyCallbackServer;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.michaelthelin.spotify.SpotifyApi;

import java.net.URI;

@Configuration
public class SpotifyClientConfig {


    @Bean
    public SpotifyApi spotifyApi(Dotenv dotenv) {

        return new SpotifyApi.Builder()
                .setClientId(dotenv.get("SPOTIFY_CLIENT_ID"))
                .setRedirectUri(URI.create(dotenv.get("SPOTIFY_REDIRECT_URI")))
                .build();
    }

    @Bean
    public SpotifyAuthService spotifyAuthService(SpotifyApi spotifyApi,
                                                 SpotifyCallbackServer spotifyCallbackServer,
                                                 SpotifyAuthorizationGenerator authorizationGenerator) {
        return new SpotifyAuthService(spotifyApi, spotifyCallbackServer, authorizationGenerator);
    }


    @Bean
    public SpotifyMusicService spotifyMusicService(SpotifyAuthService spotifyAuthService) {
        return new SpotifyMusicService(spotifyAuthService);
    }

    @Bean
    public SpotifyApi spotifyClientCredentialsApi(Dotenv dotenv){
        return new SpotifyApi.Builder()
                .setClientId(dotenv.get("SPOTIFY_CLIENT_ID"))
                .setClientSecret(dotenv.get("SPOTIFY_CLIENT_SECRET"))
                .build();
    }

    @Bean
    public SpotifyClientCredentialsService spotifyClientCredentialsService(SpotifyApi spotifyClientCredentialsApi){
        return new SpotifyClientCredentialsService(spotifyClientCredentialsApi);
    }

    @Bean
    public SpotifyCallbackServer spotifyCallbackServer(){
        return new SpotifyCallbackServer();
    }

    @Bean
    public SpotifyAuthorizationGenerator spotifyAuthorizationGenerator(){
        return new SpotifyAuthorizationGenerator();
    }

}
