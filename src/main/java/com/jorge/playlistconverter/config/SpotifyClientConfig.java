package com.jorge.playlistconverter.config;

import com.jorge.playlistconverter.spotify.SpotifyAuthService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.michaelthelin.spotify.SpotifyApi;

import java.net.URI;

@Configuration
public class SpotifyClientConfig {

    @Bean
    public SpotifyApi spotifyApi() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        return new SpotifyApi.Builder()
                .setClientId(dotenv.get("SPOTIFY_CLIENT_ID"))
                .setRedirectUri(URI.create(dotenv.get("SPOTIFY_REDIRECT_URI")))
                .build();
    }

    @Bean
    public SpotifyAuthService spotifyAuthService(SpotifyApi spotifyApi) {
        return new SpotifyAuthService(spotifyApi);
    }

}
