package com.jorge.playlistconverter.input;

import java.net.URI;
import java.util.regex.Pattern;

public class SpotifyPlaylistParser {

    private static final Pattern PLAYLIST_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9]{22}");

    private static final Pattern PLAYLIST_PATH_PATTERN =
            Pattern.compile("^/(?:intl-[a-zA-Z-]+/)?playlist/([A-Za-z0-9]{22})/?$");

    public String parse(String input){
        if(input == null || input.isBlank()){
            throw new IllegalArgumentException("Informe o link ou o ID da playlist do Spotify.");
        }

        String value = input.strip();

        if (PLAYLIST_ID_PATTERN.matcher(value).matches()) {
            return value;
        }

        URI uri = parseUri(value);

        validateSpotifyUrl(uri);

        return extractPlaylistId(uri);
    }

    private URI parseUri(String value){
        try {
            return URI.create(value);

        }catch (IllegalArgumentException e){
            throw new IllegalArgumentException("O link da playlist do Spotify é inválido.", e);
        }
    }

    private void validateSpotifyUrl(URI uri){
        boolean validUri = "https".equalsIgnoreCase(uri.getScheme())
                && "open.spotify.com".equalsIgnoreCase(uri.getHost())
                && uri.getUserInfo() == null
                && uri.getPort() == -1;

        if (!validUri){
            throw new IllegalArgumentException("Informe um link HTTPS de playlist do open.spotify.com "
                    + "ou um ID válido.");
        }
    }

    private String extractPlaylistId(URI uri){
        var matcher = PLAYLIST_PATH_PATTERN.matcher(uri.getPath());

        if (!matcher.matches()){
            throw new IllegalArgumentException("O link deve identificar uma playlist do Spotify.");
        }

        return matcher.group(1);
    }
}
