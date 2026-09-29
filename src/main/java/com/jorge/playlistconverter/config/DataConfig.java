package com.jorge.playlistconverter.config;

import com.jorge.playlistconverter.state.H2SyncStateStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataConfig {

    @Bean
    public H2SyncStateStore syncStateStore() {
        return new H2SyncStateStore();
    }
}
