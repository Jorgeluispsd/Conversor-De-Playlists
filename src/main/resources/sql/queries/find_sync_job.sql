SELECT id FROM sync_job
WHERE source_playlist_id = ?
  AND destination_playlist_id = ?
  AND direction = ?
ORDER BY id