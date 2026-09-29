MERGE INTO synced_track
(sync_job_id, source_track_id, source_title, source_artist,
 destination_track_id, match_confidence, status, synced_at)
KEY (sync_job_id, source_track_id)
VALUES (?, ?, ?, ?, ?, ?, ?, ?)