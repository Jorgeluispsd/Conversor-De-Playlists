package com.jorge.playlistconverter.state.sql;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SqlResourceLoader {

    public String read(String resourcePath){
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)){

            if (inputStream == null){
                throw new RuntimeException("Arquivo SQL não encontrado" + resourcePath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        }catch (IOException e){
            throw new RuntimeException("Erro ao ler arquivo SQL: " + resourcePath, e);
        }
    }


}
