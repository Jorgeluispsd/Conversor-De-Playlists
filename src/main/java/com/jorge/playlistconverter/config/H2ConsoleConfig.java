package com.jorge.playlistconverter.config;

import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

@Configuration
public class H2ConsoleConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2ConsoleServer() throws SQLException {
        return Server.createWebServer("-webPort", "8082");
    }
}
