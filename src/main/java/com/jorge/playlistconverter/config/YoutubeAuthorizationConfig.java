package com.jorge.playlistconverter.config;

import com.jorge.playlistconverter.youtube.authorization.GoogleAuthorizationFlowFactory;
import com.jorge.playlistconverter.youtube.authorization.GoogleClientSecretsLoader;
import com.jorge.playlistconverter.youtube.callback.GoogleCallbackServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YoutubeAuthorizationConfig {

    @Bean
    public GoogleClientSecretsLoader googleClientSecretsLoader() {
        return new GoogleClientSecretsLoader();
    }

    @Bean
    public GoogleAuthorizationFlowFactory googleAuthorizationFlowFactory(GoogleClientSecretsLoader secretsLoader) {
        return new GoogleAuthorizationFlowFactory(secretsLoader);
    }

    @Bean
    public GoogleCallbackServer googleCallbackServer() {
        return new GoogleCallbackServer();
    }
}
