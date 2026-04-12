-- ============================================================
--  JavaFXApplication7 – PostgreSQL Database Setup
--  Run this file once to create the database, tables, and
--  seed data.
--
--  HOW TO RUN:
--    psql -U YOUR_USERNAME -f setup_database.sql
--  or paste into pgAdmin Query Tool.
-- ============================================================

-- 1. Create the database (run as superuser if it doesn't exist)
-- If you already created it, comment this line out.
CREATE DATABASE pdfencryptionfolder;

-- 2. Connect to the new database
\c pdfencryptionfolder

-- ============================================================
--  TABLES
-- ============================================================

-- Patients
CREATE TABLE IF NOT EXISTS patients (
    patient_number  VARCHAR(50)  PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    dob             DATE         NOT NULL
);

-- Tests (lookup table)
CREATE TABLE IF NOT EXISTS tests (
    test_id   SERIAL      PRIMARY KEY,
    test_name VARCHAR(100) NOT NULL UNIQUE
);

-- Doctors (lookup table)
CREATE TABLE IF NOT EXISTS doctors (
    doctor_id   SERIAL      PRIMARY KEY,
    doctor_name VARCHAR(100) NOT NULL UNIQUE
);

-- Technicians (lookup table)
CREATE TABLE IF NOT EXISTS technicians (
    technician_id   SERIAL      PRIMARY KEY,
    technician_name VARCHAR(100) NOT NULL UNIQUE
);

-- Test records (main data + encrypted file)
CREATE TABLE IF NOT EXISTS test_records (
    record_id        SERIAL      PRIMARY KEY,
    patient_number   VARCHAR(50) NOT NULL REFERENCES patients(patient_number),
    test_id          INTEGER     NOT NULL REFERENCES tests(test_id),
    doctor_id        INTEGER     NOT NULL REFERENCES doctors(doctor_id),
    technician_id    INTEGER     NOT NULL REFERENCES technicians(technician_id),
    test_date        DATE        NOT NULL,
    test_status      VARCHAR(50) NOT NULL,
    encrypted_file   BYTEA,
    created_at       TIMESTAMP   DEFAULT NOW()
);

-- ============================================================
--  SEED DATA  (sample rows so dropdowns are pre-populated)
-- ============================================================

INSERT INTO tests (test_name) VALUES
    ('Blood Test'),
    ('X-Ray'),
    ('MRI'),
    ('CT Scan'),
    ('COVID-19 PCR')
ON CONFLICT (test_name) DO NOTHING;

INSERT INTO doctors (doctor_name) VALUES
    ('Dr. Smith'),
    ('Dr. Brown'),
    ('Dr. Johnson')
ON CONFLICT (doctor_name) DO NOTHING;

INSERT INTO technicians (technician_name) VALUES
    ('Tech A'),
    ('Tech B'),
    ('Tech C')
ON CONFLICT (technician_name) DO NOTHING;

-- Sample patients
INSERT INTO patients (patient_number, first_name, last_name, dob) VALUES
    ('P1001', 'John',  'Doe',   '1990-03-15'),
    ('P1002', 'Jane',  'Smith', '1985-07-22'),
    ('P1003', 'Alice', 'Brown', '2000-11-05')
ON CONFLICT (patient_number) DO NOTHING;

-- ============================================================
--  DONE
-- ============================================================
\echo '✅ Database setup complete.'
