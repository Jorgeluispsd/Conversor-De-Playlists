package com.jorge.playlistconverter.youtube.authorization;

import com.jorge.playlistconverter.errors.youtube.YoutubeAuthenticationException;


import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

public class GoogleAuthorizationBrowser {

    public void open(URI authorizationUri){
        Objects.requireNonNull(authorizationUri);

        if (!Desktop.isDesktopSupported()){
            throw new YoutubeAuthenticationException("A abertura automática do navegador "
                    + "não está disponível neste ambiente.");
        }

        Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(Desktop.Action.BROWSE)){
            throw new YoutubeAuthenticationException( "O ambiente não oferece suporte "
                    + "para abrir URLs no navegador.");
        }

        try {
            desktop.browse(authorizationUri);

        }catch (IOException e){
            throw new YoutubeAuthenticationException("Não foi possível abrir o navegador "
                    + "para a autorização Google.", e);
        }
    }
}
