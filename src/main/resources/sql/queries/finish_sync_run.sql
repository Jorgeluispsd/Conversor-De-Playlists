UPDATE sync_run
SET completed_at = CURRENT_TIMESTAMP
WHERE id = ?
  AND completed_at IS NULL;