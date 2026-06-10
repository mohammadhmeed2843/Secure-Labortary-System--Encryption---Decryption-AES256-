-- ============================================================
--  Phase 3 Migration (Command 3) — Role Model & Workflow Refactor
--  psql -U postgres -d pdfencryptionfolder -f migrate_v4.sql
-- ============================================================

-- ── Audit log table ──────────────────────────────────────────
-- Captures every meaningful user action for Admin inspection.
-- user_id and username are both stored: user_id for JOIN queries,
-- username as a denormalized fallback after account deletion.

CREATE TABLE IF NOT EXISTS audit_log (
    log_id      SERIAL       PRIMARY KEY,
    user_id     INTEGER      REFERENCES users(user_id) ON DELETE SET NULL,
    username    VARCHAR(50),
    action      VARCHAR(50)  NOT NULL,
    target_type VARCHAR(50),             -- 'file', 'user', 'session', 'system'
    target_id   VARCHAR(100),            -- file_id, user_id, etc.
    details     TEXT,
    created_at  TIMESTAMP    DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_user   ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_time   ON audit_log(created_at DESC);

-- ── Ensure file_version / previous_version_id exist ──────────
-- (were added in migrate_v3.sql; these are safe no-ops if already present)

ALTER TABLE medical_files
    ADD COLUMN IF NOT EXISTS file_version        INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS previous_version_id INTEGER
        REFERENCES medical_files(file_id) ON DELETE SET NULL;

\echo '-- migrate_v4.sql applied.'
\echo '-- audit_log table created with indexes.'
\echo '-- file_version / previous_version_id confirmed on medical_files.'
