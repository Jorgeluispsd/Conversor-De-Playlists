package com.jorge.playlistconverter.youtube.authorization;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;

import java.util.Objects;

public class YoutubeAuthenticatedClientFactory {

    private final GoogleAuthService authService;

    public YoutubeAuthenticatedClientFactory(GoogleAuthService authService){
        this.authService = Objects.requireNonNull(authService);
    }

    public YouTube create(){
        Credential credential = authService.ensureAuthenticated();

        return new YouTube.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("playlist-converter")
                .build();
    }
}
