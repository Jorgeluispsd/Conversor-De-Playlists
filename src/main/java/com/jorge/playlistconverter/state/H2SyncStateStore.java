package com.jorge.playlistconverter.state;

import com.jorge.playlistconverter.model.Song;
import java.util.OptionalLong;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class H2SyncStateStore implements SyncStateStore{

    private static final String SCHEMA_PATH = "sql/schema.sql";
    private static final String IS_ALREADY_SYNCED_PATH = "sql/queries/is_already_synced.sql";
    private static final String MARK_SYNCED_PATH = "sql/queries/mark_synced.sql";
    private static final String GET_HISTORY_PATH = "sql/queries/get_history.sql";
    private static final String CREATE_SYNC_JOB_PATH = "sql/queries/create_sync_job.sql";
    private static final String FIND_SYNC_JOB_PATH = "sql/queries/find_sync_job.sql";

    private final String dbPath;

    private final String isAlreadySyncedSql;
    private final String markSyncedSql;
    private final String getHistorySql;
    private final String createSyncJobSql;
    private final String findSyncJobSql;

    public H2SyncStateStore(){
        String homeDir = System.getProperty("user.home");
        Path appDir = Paths.get(homeDir, ".playlist-converter");

        try {
            Files.createDirectories(appDir);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar diretório de dados", e);
        }

        this.dbPath = appDir.resolve("data.db").toString();
        initializeDatabase();

        this.isAlreadySyncedSql = readSqlFile(IS_ALREADY_SYNCED_PATH);
        this.markSyncedSql = readSqlFile(MARK_SYNCED_PATH);
        this.getHistorySql = readSqlFile(GET_HISTORY_PATH);
        this.createSyncJobSql = readSqlFile(CREATE_SYNC_JOB_PATH);
        this.findSyncJobSql = readSqlFile(FIND_SYNC_JOB_PATH);
    }

    private void initializeDatabase() {
        String sql = readSqlFile(SCHEMA_PATH);

        try(Connection conn = getConnection();
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
        return DriverManager.getConnection("jdbc:h2:" + dbPath);
    }

    @Override
    public long createSyncJob(String sourcePlaylistId, String destinationPlaylistId, String direction){
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(createSyncJobSql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, sourcePlaylistId);
            pstmt.setString(2, destinationPlaylistId);
            pstmt.setString(3, direction);
            pstmt.executeUpdate();

            try(ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()){
                    return keys.getLong(1);
                }
                throw new SQLException("Nenhum ID gerado para o novo sync_job");
            }

        }catch (SQLException e) {
            throw new RuntimeException("Erro ao criar sync_job", e);
        }
    }

    @Override
    public boolean isAlreadySynced(long syncJobId, String sourceTrackId){
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(isAlreadySyncedSql)) {

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
    public void markSynced(long syncJobId, Song sourceSong, String destinationId, String status, double confidence){

        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(markSyncedSql)) {

            pstmt.setLong(1, syncJobId);
            pstmt.setString(2, sourceSong.id());
            pstmt.setString(3, sourceSong.title());
            pstmt.setString(4, sourceSong.artist());
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
        List<String> history = new ArrayList<>();

        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(getHistorySql)) {

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

    @Override
    public OptionalLong findSyncJob(String sourcePlaylistId, String destinationPlaylistId, String direction){
        try (Connection conn = getConnection();
        PreparedStatement statement = conn.prepareStatement(findSyncJobSql)){

            statement.setString(1, sourcePlaylistId);
            statement.setString(2, destinationPlaylistId);
            statement.setString(3, direction);

            try (ResultSet result = statement.executeQuery()) {

                return result.next()
                        ? OptionalLong.of(result.getLong("id"))
                        : OptionalLong.empty();
            }

        }catch (SQLException e){
            throw new RuntimeException("Erro ao buscar trabalho de sincronização", e);
        }
    }
}
