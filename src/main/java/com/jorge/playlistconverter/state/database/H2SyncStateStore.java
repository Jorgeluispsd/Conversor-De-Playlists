package com.jorge.playlistconverter.state.database;

import com.jorge.playlistconverter.enums.SyncRunStatus;
import com.jorge.playlistconverter.errors.persistence.SyncStateStorageException;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.state.SyncStateStore;
import com.jorge.playlistconverter.state.sql.SqlResourceLoader;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

public class H2SyncStateStore implements SyncStateStore {

    private static final String IS_ALREADY_SYNCED_PATH = "sql/queries/is_already_synced.sql";
    private static final String MARK_SYNCED_PATH = "sql/queries/mark_synced.sql";
    private static final String GET_HISTORY_PATH = "sql/queries/get_history.sql";
    private static final String CREATE_SYNC_JOB_PATH = "sql/queries/create_sync_job.sql";
    private static final String FIND_SYNC_JOB_PATH = "sql/queries/find_sync_job.sql";
    private static final String START_SYNC_RUN_PATH = "sql/queries/start_sync_run.sql";
    private static final String FINISH_SYNC_RUN_PATH = "sql/queries/finish_sync_run.sql";

    private final String dbPath;

    private final String isAlreadySyncedSql;
    private final String markSyncedSql;
    private final String getHistorySql;
    private final String createSyncJobSql;
    private final String findSyncJobSql;
    private final String startSyncRunSql;
    private final String finishSyncRunSql;

    public H2SyncStateStore(SqlResourceLoader sqlResourceLoader, H2DatabaseInitializer databaseInitializer){
        this.dbPath = databaseInitializer.initialize();

        this.isAlreadySyncedSql = sqlResourceLoader.read(IS_ALREADY_SYNCED_PATH);
        this.markSyncedSql = sqlResourceLoader.read(MARK_SYNCED_PATH);
        this.getHistorySql = sqlResourceLoader.read(GET_HISTORY_PATH);
        this.createSyncJobSql = sqlResourceLoader.read(CREATE_SYNC_JOB_PATH);
        this.findSyncJobSql = sqlResourceLoader.read(FIND_SYNC_JOB_PATH);
        this.startSyncRunSql = sqlResourceLoader.read(START_SYNC_RUN_PATH);
        this.finishSyncRunSql = sqlResourceLoader.read(FINISH_SYNC_RUN_PATH);
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
            throw new SyncStateStorageException("Erro ao criar sync_job", e);
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
            throw new SyncStateStorageException("Erro ao verificar se a faixa já foi sincronizada", e);
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
            throw new SyncStateStorageException("Erro ao marcar a faixa como sincronizada", e);
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
            throw new SyncStateStorageException("Erro ao obter o histórico de sincronização", e);
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
            throw new SyncStateStorageException("Erro ao buscar trabalho de sincronização", e);
        }
    }

    @Override
    public long getOrCreateSyncJob(String sourcePlaylistId, String destinationPlaylistId, String direction){
        OptionalLong existing = findSyncJob(sourcePlaylistId, destinationPlaylistId, direction);

        if(existing.isPresent()){
            return existing.getAsLong();
        }

        try {
            return createSyncJob(sourcePlaylistId, destinationPlaylistId, direction);
        }catch (SyncStateStorageException e){
            if (!isDuplicatedKey(e)){
                throw e;
            }

            return findSyncJob(sourcePlaylistId, destinationPlaylistId, direction)
                    .orElseThrow(() -> e);
        }
    }

    private boolean isDuplicatedKey(SyncStateStorageException exception){
        return exception.getCause() instanceof SQLException sqlException
                && "23505".equals(sqlException.getSQLState());
    }

    @Override
    public long startSyncRun(long syncJobId){
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(startSyncRunSql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, syncJobId);
            pstmt.executeUpdate();

            try(ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()){
                    return keys.getLong(1);
                }

                throw new SQLException("Nenhum Id gerado para o sync_run");
            }

        }catch (SQLException e){
            throw new SyncStateStorageException("Erro ao iniciar execução de sincronização", e);
        }
    }

    @Override
    public void finishSyncRun(long syncRunId,
                              SyncRunStatus status){

        Objects.requireNonNull(status, "O status da execução não pode ser nulo");

        if (status == SyncRunStatus.RUNNING){
            throw new IllegalArgumentException("Para finalizar, informe SUCCESS ou FAILED");
        }

        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(finishSyncRunSql)) {

            pstmt.setString(1, status.name());
            pstmt.setLong(2, syncRunId);

            int updatedRows = pstmt.executeUpdate();

            if (updatedRows == 0){
                throw new IllegalStateException(
                        "Execução inexistente ou já finalizada: " + syncRunId
                );
            }

        }catch (SQLException e){
            throw new SyncStateStorageException("Erro ao finalizar execução de sincronização: ", e);
        }
    }
}
