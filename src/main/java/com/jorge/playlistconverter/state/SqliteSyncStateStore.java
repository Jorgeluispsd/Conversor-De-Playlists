package com.jorge.playlistconverter.state;

import com.jorge.playlistconverter.model.Track;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteSyncStateStore implements SyncStateStore{

    private final String dbPath;

    public SqliteSyncStateStore(){
        String homeDir = System.getProperty("user.home");
        Path appDir = Paths.get(homeDir, ".playlist-converter");

        try {
            Files.createDirectories(appDir);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar diretório de dados", e);
        }

        this.dbPath = appDir.resolve("data.db").toString();
        initializeDatabase();
    }

    private void initializeDatabase() {
        String sql = readSqlFile("sql/schema.sql");

        try(Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement stmt = conn.createStatement()) {

            for (String statement : sql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement.trim());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar o banco de dados", e);
        }
    }


    private String readSqlFile(String resourcePath){

        try(InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null){
                throw new RuntimeException("Arquivo SQL não encontrado" + resourcePath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo de SQL"+ resourcePath, e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    @Override
    public boolean isAlreadySynced(long syncJobId, String sourceTrackId){
        String sql = readSqlFile("sql/queries/is_already_synced.sql");

        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, syncJobId);
            pstmt.setString(2, sourceTrackId);

            try(ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        }catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar se a faixa já foi sincronizada", e);
        }
    }


    @Override
    public void markSynced(long syncJobId, Track sourceTrack, String destinationId, String status,double confidence){
        String sql = readSqlFile("sql/queries/mark_synced.sql");


        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, syncJobId);
            pstmt.setString(2, sourceTrack.id());
            pstmt.setString(3, sourceTrack.title());
            pstmt.setString(4, sourceTrack.artist());
            pstmt.setString(5, destinationId);
            pstmt.setDouble(6, confidence);
            pstmt.setString(7, status);
            pstmt.setString(8, java.time.Instant.now().toString());

            pstmt.executeUpdate();
        }catch (SQLException e) {
            throw new RuntimeException("Erro ao marcar a faixa como sincronizada", e);
        }
    }

    @Override
    public List<String> getHistory(long syncJobId){
        String sql = readSqlFile("sql/queries/get_history.sql");
        List<String> history = new ArrayList<>();

        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, syncJobId);

            try(ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()){
                    history.add(rs.getString("source_track_id"));
                }
            }

        }catch (SQLException e) {
            throw new RuntimeException("Erro ao obter o histórico de sincronização", e);
        }

        return history;
    }
}
