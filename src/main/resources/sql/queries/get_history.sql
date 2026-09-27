SELECT source_track_id FROM synced_track
WHERE sync_job_id = ?
ORDER BY synced_at DESC