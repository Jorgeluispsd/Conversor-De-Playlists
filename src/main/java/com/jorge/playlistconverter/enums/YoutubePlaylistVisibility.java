package com.jorge.playlistconverter.enums;

import lombok.Getter;

public enum YoutubePlaylistVisibility {
    PRIVATE("private"),
    PUBLIC("public"),
    UNLISTED("unlisted");;

    @Getter
    private final String apiValue;

    YoutubePlaylistVisibility(String apiValue){
        this.apiValue = apiValue;
    }

}
