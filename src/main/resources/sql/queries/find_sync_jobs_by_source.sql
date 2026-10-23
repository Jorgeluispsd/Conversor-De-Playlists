SELECT id,
       source_playlist_id,
       destination_playlist_id,
       direction
FROM sync_job
WHERE source_playlist_id = ?
  AND direction = ?
ORDER BY id;