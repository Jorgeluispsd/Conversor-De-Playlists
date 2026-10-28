package com.jorge.playlistconverter.input;

import com.jorge.playlistconverter.errors.input.InvalidPlaylistInputException;

import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;

import static com.jorge.playlistconverter.commons.http.QueryParameterParser.extractParam;

public class YoutubePlaylistParser {

    private static final Pattern PLAYLIST_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9_-]+");

    private static final Set<String> ALLOWED_HOSTS =
                Set.of("youtube.com",
                        "www.youtube.com",
                        "music.youtube.com");

    public String parse(String input){
        if (input == null || input.isBlank()){
            throw new InvalidPlaylistInputException(
                    "Informe o link ou o ID da playlist do YouTube.");
        }

        String value = input.strip();

        if (PLAYLIST_ID_PATTERN.matcher(value).matches()){
            return value;
        }

        URI uri = parseUri(value);

        validateYoutubeUrl(uri);

        return extractPlaylistId(uri);
    }

    private URI parseUri(String value){
        try {
            return URI.create(value);

        }catch (IllegalArgumentException e){
            throw new InvalidPlaylistInputException(
                    "O link da playlist do YouTube é inválido.", e);
        }
    }

    private void validateYoutubeUrl(URI uri){
        String host = uri.getHost();

        boolean validHost = host != null
                && ALLOWED_HOSTS.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(host));

        boolean validUrl = "https".equalsIgnoreCase(uri.getScheme())
                && validHost
                && uri.getUserInfo() == null
                && uri.getPort() == -1;

        if (!validUrl){
            throw new InvalidPlaylistInputException(
                    "Informe um link HTTPS de playlist do YouTube "
                            + "ou um ID válido.");
        }

        String path = uri.getPath();

        if (!"/playlist".equals(path) && !"/watch".equals(path)){
            throw new InvalidPlaylistInputException(
                    "O link deve identificar uma playlist do YouTube.");
        }
    }

    private String extractPlaylistId(URI uri){
        String playlistId = extractParam(uri.getRawQuery(), "list");

        if (playlistId == null
                || !PLAYLIST_ID_PATTERN.matcher(playlistId).matches()){
            throw new InvalidPlaylistInputException(
                    "O link deve conter um ID de playlist válido "
                            + "no parâmetro list.");
        }

        return playlistId;
    }
}
