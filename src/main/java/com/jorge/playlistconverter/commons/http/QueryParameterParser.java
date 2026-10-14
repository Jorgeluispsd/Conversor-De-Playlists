package com.jorge.playlistconverter.commons.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class QueryParameterParser {

    private QueryParameterParser() {
    }

    public static String extractParam(String query, String key){
        if (query == null || query.isBlank()){
            return null;
        }

        for (String pair : query.split("&")){
            String[] parts= pair.split("=", 2);

            String name = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8);

            if (key.equals(name)){
                return parts.length == 2
                        ? URLDecoder.decode(
                        parts[1], StandardCharsets.UTF_8)
                        : "";
            }
        }

        return null;
    }
}
