package com.jorge.playlistconverter.enums;

import lombok.Getter;

public enum PlaylistVisibility {
    PRIVATE("private"),
    PUBLIC("public"),
    UNLISTED("unlisted");;

    @Getter
    private final String apiValue;

    PlaylistVisibility(String apiValue){
        this.apiValue = apiValue;
    }

}
