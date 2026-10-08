package com.jorge.playlistconverter.config;

import com.jorge.playlistconverter.state.H2SyncStateStore;
import com.jorge.playlistconverter.state.sql.SqlResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataConfig {

    @Bean
    public H2SyncStateStore syncStateStore(SqlResourceLoader sqlResourceLoader) {
        return new H2SyncStateStore(sqlResourceLoader);
    }

    @Bean
    public SqlResourceLoader sqlResourceLoader() {
        return new SqlResourceLoader();
    }
}
