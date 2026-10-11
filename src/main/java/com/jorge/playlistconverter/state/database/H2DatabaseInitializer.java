package com.jorge.playlistconverter.state.database;

import com.jorge.playlistconverter.errors.persistence.DatabaseInitializationException;
import com.jorge.playlistconverter.state.sql.SqlResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class H2DatabaseInitializer {

    private static final String SCHEMA_PATH = "sql/schema.sql";

    private final SqlResourceLoader sqlResourceLoader;

    public H2DatabaseInitializer(SqlResourceLoader sqlResourceLoader) {
        this.sqlResourceLoader = sqlResourceLoader;
    }

    public String initialize(){
        String homeDir = System.getProperty("user.home");
        Path appDir = Paths.get(homeDir, ".playlist-converter");

        try {
            Files.createDirectories(appDir);

        }catch (IOException e){
            throw new DatabaseInitializationException(
                    "Não foi possível preparar o diretório de dados da aplicação.", e);
        }

        String dbPath = appDir.resolve("data.db").toString();

        initializeSchema(dbPath);

        return dbPath;
    }

    private void initializeSchema(String dbPath){
        String sql = sqlResourceLoader.read(SCHEMA_PATH);

        try (Connection connection = DriverManager.getConnection("jdbc:h2:" + dbPath);
             Statement statement = connection.createStatement()) {

            for (String command : sql.split(";")){
                if (!command.trim().isEmpty()){
                    statement.execute(command);
                }
            }

        }catch (SQLException e){
            throw new DatabaseInitializationException("Não foi possível inicializar a estrutura do banco H2.", e);
        }
    }

}
