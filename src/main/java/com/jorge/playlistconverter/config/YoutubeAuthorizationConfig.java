package com.jorge.playlistconverter.config;

import com.jorge.playlistconverter.youtube.authorization.GoogleClientSecretsLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YoutubeAuthorizationConfig {

    @Bean
    public GoogleClientSecretsLoader googleClientSecretsLoader() {
        return new GoogleClientSecretsLoader();
    }
}
