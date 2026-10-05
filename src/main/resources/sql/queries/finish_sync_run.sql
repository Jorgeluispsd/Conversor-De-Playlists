UPDATE sync_run
SET status = ?,
    completed_at = CURRENT_TIMESTAMP
WHERE id = ?
  AND status = 'RUNNING'
  AND completed_at IS NULL;