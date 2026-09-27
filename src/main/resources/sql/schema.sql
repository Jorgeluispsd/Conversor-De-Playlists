CREATE TABLE IF NOT EXISTS sync_job (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_playlist_id TEXT NOT NULL,
    destination_playlist_id TEXT NOT NULL,
    direction TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sync_run (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sync_job_id INTEGER NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (sync_job_id) REFERENCES sync_job(id)
    );

CREATE TABLE IF NOT EXISTS synced_track (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sync_job_id INTEGER NOT NULL,
    source_track_id TEXT NOT NULL,
    source_title TEXT,
    source_artist TEXT,
    destination_track_id TEXT NOT NULL,
    match_confidence REAL,
    status TEXT,
    synced_at TIMESTAMP,
    FOREIGN KEY (sync_job_id) REFERENCES sync_job(id),
    UNIQUE(sync_job_id, source_track_id)
    );