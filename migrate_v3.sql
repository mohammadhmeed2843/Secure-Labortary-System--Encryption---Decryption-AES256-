-- ============================================================
--  Phase 3 Migration (Command 2) — Backend & Security Refactor
--  psql -U postgres -d pdfencryptionfolder -f migrate_v3.sql
-- ============================================================

-- ── 1. Rename TECHNICIAN role → RECEPTIONIST ────────────────

-- Drop the old CHECK constraint that includes 'TECHNICIAN'
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- Add the new constraint with RECEPTIONIST in place of TECHNICIAN
ALTER TABLE users
    ADD CONSTRAINT users_role_check
    CHECK (role IN ('ADMIN', 'RECEPTIONIST', 'DOCTOR'));

-- Migrate any existing TECHNICIAN rows
UPDATE users SET role = 'RECEPTIONIST' WHERE role = 'TECHNICIAN';

-- ── 2. Drop Phase 1 orphan table ─────────────────────────────

-- test_records was used before medical_files was introduced.
-- No application code references it; dropping it reduces confusion.
DROP TABLE IF EXISTS test_records;

-- ── 3. Add version-tracking columns to medical_files ─────────
-- These columns lay the groundwork for a file-history/revision
-- system that will be surfaced in Command 3/4.
-- file_version : which revision of the record this is (starts at 1)
-- previous_version_id : points to the file_id this record replaced

ALTER TABLE medical_files
    ADD COLUMN IF NOT EXISTS file_version        INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS previous_version_id INTEGER
        REFERENCES medical_files(file_id) ON DELETE SET NULL;

-- ── 4. Ensure default receptionist user exists ───────────────
-- The Java AuthService will re-seed on first launch if the
-- table is empty, but if you are migrating an existing DB
-- that already has a 'technician' user, rename the username too.

UPDATE users
SET username  = 'receptionist',
    full_name = 'Lab Receptionist'
WHERE username = 'technician';

-- ── Done ─────────────────────────────────────────────────────

\echo '-- migrate_v3.sql applied successfully.'
\echo '-- TECHNICIAN → RECEPTIONIST in constraint and data.'
\echo '-- test_records table dropped.'
\echo '-- file_version / previous_version_id columns added to medical_files.'
