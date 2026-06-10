-- ============================================================
--  Phase 2 Migration — Run against existing pdfencryptionfolder DB
--  psql -U postgres -d pdfencryptionfolder -f migrate_v2.sql
-- ============================================================

-- Users table: PBKDF2-hashed credentials + role
CREATE TABLE IF NOT EXISTS users (
    user_id       SERIAL       PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt          VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN','TECHNICIAN','DOCTOR')),
    full_name     VARCHAR(100) NOT NULL,
    active        BOOLEAN      DEFAULT TRUE,
    created_at    TIMESTAMP    DEFAULT NOW()
);

-- Medical files: encrypted blob + all metadata in one table
-- Replaces the test_records approach of mixing blob with metadata
CREATE TABLE IF NOT EXISTS medical_files (
    file_id        SERIAL      PRIMARY KEY,
    patient_number VARCHAR(50) NOT NULL REFERENCES patients(patient_number),
    encrypted_data BYTEA       NOT NULL,
    original_name  VARCHAR(255),
    test_id        INTEGER     REFERENCES tests(test_id),
    doctor_id      INTEGER     REFERENCES doctors(doctor_id),
    technician_id  INTEGER     REFERENCES technicians(technician_id),
    test_date      DATE,
    test_status    VARCHAR(50) DEFAULT 'Pending',
    status         VARCHAR(20) DEFAULT 'READY' CHECK (status IN ('READY','VIEWED','ARCHIVED')),
    uploaded_by    INTEGER     REFERENCES users(user_id),
    uploaded_at    TIMESTAMP   DEFAULT NOW()
);

-- File keys: per-file DEK (wrapped with master KEK) + IV
-- Completely separate table — only FileService joins these two
CREATE TABLE IF NOT EXISTS file_keys (
    file_id       INTEGER PRIMARY KEY REFERENCES medical_files(file_id) ON DELETE CASCADE,
    encrypted_key BYTEA   NOT NULL,   -- DEK wrapped with application master key
    iv            BYTEA   NOT NULL,   -- 16-byte AES-CBC IV
    algorithm     VARCHAR(50) DEFAULT 'AES/CBC/PKCS5Padding',
    created_at    TIMESTAMP DEFAULT NOW()
);

\echo '-- Phase 2 schema applied.'
\echo '-- Default users will be seeded automatically on first application launch.'
\echo '-- Old test_records table is preserved; stop using it for new records.'
