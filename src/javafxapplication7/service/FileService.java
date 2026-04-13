package javafxapplication7.service;

import javafxapplication7.DatabaseConnection;
import javafxapplication7.model.FileRecord;
import javafxapplication7.model.KeyRecord;
import javafxapplication7.model.RecordDraft;
import javafxapplication7.session.Session;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Primary service for the encrypted file lifecycle.
 *
 * Upload flow  (uploadAndEncrypt):
 *   read plaintext → generate DEK+IV → encrypt → DB transaction
 *   [upsert patient → insert medical_files → insert file_keys] → commit
 *   → secure-delete original ONLY after confirmed commit
 *
 * Export/View flow  (exportDecrypted):
 *   load ciphertext from medical_files → load wrapped DEK+IV from file_keys
 *   → unwrap DEK → decrypt → write to output path → mark VIEWED
 *
 * Controllers call only the public API; no SQL or crypto logic belongs there.
 *
 * Bug fixes applied (Command 2):
 *   - loadRecordWithData: replaced fragile String.replace() with a dedicated
 *     SQL constant (BASE_SELECT_WITH_BLOB).
 *   - queryList: replaced parse-by-exception integer detection with explicit
 *     typed overloads (queryAll, queryByString, queryById).
 */
public final class FileService {

    // ── SQL constants ─────────────────────────────────────────────────────────

    /**
     * Base SELECT that loads all metadata columns but NOT the encrypted blob.
     * Used for list views where loading large BLOBs is wasteful.
     */
    private static final String BASE_SELECT =
        "SELECT mf.file_id, mf.patient_number, " +
        "       p.first_name || ' ' || p.last_name AS patient_name, " +
        "       mf.original_name, " +
        "       t.test_name, d.doctor_name, tech.technician_name, " +
        "       mf.test_date, mf.test_status, mf.status, " +
        "       mf.uploaded_by, u.full_name AS uploader_name, mf.uploaded_at, " +
        "       mf.file_version, mf.previous_version_id " +
        "FROM medical_files mf " +
        "JOIN patients p        ON p.patient_number   = mf.patient_number " +
        "LEFT JOIN tests        t    ON t.test_id          = mf.test_id " +
        "LEFT JOIN doctors      d    ON d.doctor_id        = mf.doctor_id " +
        "LEFT JOIN technicians  tech ON tech.technician_id = mf.technician_id " +
        "LEFT JOIN users        u    ON u.user_id          = mf.uploaded_by ";

    /**
     * Variant that also fetches encrypted_data for export/view operations.
     * Kept as a separate constant — no fragile string manipulation.
     */
    private static final String BASE_SELECT_WITH_BLOB =
        "SELECT mf.file_id, mf.patient_number, " +
        "       p.first_name || ' ' || p.last_name AS patient_name, " +
        "       mf.encrypted_data, mf.original_name, " +
        "       t.test_name, d.doctor_name, tech.technician_name, " +
        "       mf.test_date, mf.test_status, mf.status, " +
        "       mf.uploaded_by, u.full_name AS uploader_name, mf.uploaded_at, " +
        "       mf.file_version, mf.previous_version_id " +
        "FROM medical_files mf " +
        "JOIN patients p        ON p.patient_number   = mf.patient_number " +
        "LEFT JOIN tests        t    ON t.test_id          = mf.test_id " +
        "LEFT JOIN doctors      d    ON d.doctor_id        = mf.doctor_id " +
        "LEFT JOIN technicians  tech ON tech.technician_id = mf.technician_id " +
        "LEFT JOIN users        u    ON u.user_id          = mf.uploaded_by ";

    // ── Upload ────────────────────────────────────────────────────────────────

    /**
     * Full atomic upload:
     *   encrypts the file → saves patient + medical_files + file_keys in one
     *   transaction → securely deletes the original ONLY after commit succeeds.
     *
     * If anything fails before commit the original file is untouched.
     *
     * @return the newly created file_id
     * @throws Exception on any failure; original file is NOT deleted on failure
     */
    public static int uploadAndEncrypt(RecordDraft draft) throws Exception {
        if (!draft.hasFile()) throw new IllegalArgumentException("No file in draft.");

        File   original = draft.getOriginalFile();
        byte[] raw      = Files.readAllBytes(original.toPath());

        // Per-file DEK and IV — generated fresh, never reused, never exposed
        SecretKey dek        = CryptoService.generateDEK();
        byte[]    iv         = CryptoService.generateIV();
        byte[]    ciphertext = CryptoService.encrypt(raw, dek, iv);
        byte[]    wrappedDEK = CryptoService.wrapDEK(dek);

        int uploadedBy = Session.isLoggedIn() ? Session.getUser().getUserId() : 0;

        int fileId;
        try (Connection conn = DatabaseConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                PatientService.upsert(conn, draft.getPatient());

                int testId = LookupService.getOrCreateTest(conn, draft.getTestType());
                int docId  = LookupService.getOrCreateDoctor(conn, draft.getDoctorName());
                int techId = LookupService.getOrCreateTechnician(conn, draft.getTechnicianName());

                fileId = insertFile(conn, draft, ciphertext, testId, docId, techId, uploadedBy);
                KeyService.save(conn, fileId, wrappedDEK, iv);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }

        // Original file is deleted ONLY after the transaction has committed
        secureDelete(original);
        AuditService.logCurrent(AuditService.UPLOAD, "file",
                String.valueOf(fileId), draft.getOriginalFile().getName());
        return fileId;
    }

    /**
     * Uploads a new version of an existing record.
     * The previous active file is archived and linked via previous_version_id.
     *
     * @param draft          new record content (same patient is assumed)
     * @param previousFileId file_id of the version being superseded
     * @return the newly created file_id
     */
    public static int uploadNewVersion(RecordDraft draft, int previousFileId) throws Exception {
        if (!draft.hasFile()) throw new IllegalArgumentException("No file in draft.");

        // Determine next version number
        FileRecord prev = loadRecord(previousFileId);
        int nextVersion = prev.getFileVersion() + 1;

        File   original = draft.getOriginalFile();
        byte[] raw      = Files.readAllBytes(original.toPath());

        SecretKey dek        = CryptoService.generateDEK();
        byte[]    iv         = CryptoService.generateIV();
        byte[]    ciphertext = CryptoService.encrypt(raw, dek, iv);
        byte[]    wrappedDEK = CryptoService.wrapDEK(dek);

        int uploadedBy = Session.isLoggedIn() ? Session.getUser().getUserId() : 0;

        int fileId;
        try (Connection conn = DatabaseConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                PatientService.upsert(conn, draft.getPatient());

                int testId = LookupService.getOrCreateTest(conn, draft.getTestType());
                int docId  = LookupService.getOrCreateDoctor(conn, draft.getDoctorName());
                int techId = LookupService.getOrCreateTechnician(conn, draft.getTechnicianName());

                // Archive the previous version
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE medical_files SET status = 'ARCHIVED' WHERE file_id = ?")) {
                    ps.setInt(1, previousFileId);
                    ps.executeUpdate();
                }

                fileId = insertFileVersioned(conn, draft, ciphertext,
                        testId, docId, techId, uploadedBy,
                        nextVersion, previousFileId);
                KeyService.save(conn, fileId, wrappedDEK, iv);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }

        secureDelete(original);
        AuditService.logCurrent(AuditService.UPDATE_FILE, "file",
                String.valueOf(fileId),
                "v" + nextVersion + " supersedes file_id=" + previousFileId);
        return fileId;
    }

    // ── Export / View ─────────────────────────────────────────────────────────

    /**
     * Decrypts the stored file and writes it to outputDir.
     * The record's status is updated to VIEWED after a successful export.
     * The decrypted copy is the caller's responsibility (temp dir + deleteOnExit
     * for view-only usage; permanent path for admin export).
     *
     * @return the written File so the caller can open or display it
     */
    public static File exportDecrypted(int fileId, File outputDir) throws Exception {
        FileRecord record    = loadRecordWithData(fileId);
        KeyRecord  keyRecord = KeyService.load(fileId);

        SecretKey dek       = CryptoService.unwrapDEK(keyRecord.getEncryptedKey());
        byte[]    plaintext = CryptoService.decrypt(record.getEncryptedData(), dek, keyRecord.getIv());

        outputDir.mkdirs();
        String fileName = (record.getOriginalName() != null && !record.getOriginalName().isBlank())
                ? record.getOriginalName()
                : "record_" + fileId + ".pdf";

        File out = new File(outputDir, fileName);
        Files.write(out.toPath(), plaintext);

        markViewed(fileId);
        AuditService.logCurrent(AuditService.EXPORT, "file",
                String.valueOf(fileId), fileName);
        return out;
    }

    // ── Public Query API ──────────────────────────────────────────────────────

    /** All records (no blob), newest first. */
    public static List<FileRecord> listAll() throws Exception {
        return queryAll(BASE_SELECT + "ORDER BY mf.uploaded_at DESC");
    }

    /** All records for one patient (no blob), newest first by test date. */
    public static List<FileRecord> listForPatient(String patientNumber) throws Exception {
        return queryByString(
            BASE_SELECT + "WHERE mf.patient_number = ? ORDER BY mf.test_date DESC",
            patientNumber);
    }

    /** Records uploaded by a specific user (no blob), newest first. */
    public static List<FileRecord> listByUploader(int userId) throws Exception {
        return queryById(
            BASE_SELECT + "WHERE mf.uploaded_by = ? ORDER BY mf.uploaded_at DESC",
            userId);
    }

    /** Single record metadata (no blob). Throws if not found. */
    public static FileRecord loadRecord(int fileId) throws Exception {
        List<FileRecord> list = queryById(
            BASE_SELECT + "WHERE mf.file_id = ?", fileId);
        if (list.isEmpty()) throw new Exception("Record not found: file_id=" + fileId);
        return list.get(0);
    }

    /** Total record count across all patients. */
    public static int countAll() {
        return countWhere("1=1");
    }

    /** Count records matching a specific status (READY, VIEWED, ARCHIVED). */
    public static int countByStatus(String status) {
        return countWhere("status = '" + status.replace("'", "") + "'");
    }

    /** Count records uploaded by a specific user. */
    public static int countByUploader(int userId) {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM medical_files WHERE uploaded_by = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception ignored) { return 0; }
    }

    /** Updates the status of a record (READY → VIEWED → ARCHIVED). */
    public static void updateStatus(int fileId, String status) {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE medical_files SET status = ? WHERE file_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, fileId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[FileService] updateStatus failed: " + e.getMessage());
        }
    }

    // ── Private Query Helpers ─────────────────────────────────────────────────

    /** Run a no-parameter SELECT and return the mapped list. */
    private static List<FileRecord> queryAll(String sql) throws Exception {
        List<FileRecord> out = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapRow(rs, false));
        }
        return out;
    }

    /** Run a VARCHAR-parameter SELECT (e.g. patient_number). */
    private static List<FileRecord> queryByString(String sql, String param) throws Exception {
        List<FileRecord> out = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(mapRow(rs, false));
        }
        return out;
    }

    /** Run an INTEGER-parameter SELECT (e.g. file_id, uploaded_by). */
    private static List<FileRecord> queryById(String sql, int id) throws Exception {
        List<FileRecord> out = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(mapRow(rs, false));
        }
        return out;
    }

    /** Loads encrypted blob — only called internally by exportDecrypted(). */
    private static FileRecord loadRecordWithData(int fileId) throws Exception {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                 BASE_SELECT_WITH_BLOB + "WHERE mf.file_id = ?")) {
            ps.setInt(1, fileId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs, true);
        }
        throw new Exception("File record not found: file_id=" + fileId);
    }

    private static int countWhere(String whereClause) {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM medical_files WHERE " + whereClause);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception ignored) { return 0; }
    }

    // ── Private Write Helpers ─────────────────────────────────────────────────

    private static int insertFile(Connection conn, RecordDraft draft,
                                  byte[] ciphertext, int testId, int docId,
                                  int techId, int uploadedBy) throws Exception {
        final String sql =
            "INSERT INTO medical_files " +
            "(patient_number, encrypted_data, original_name, " +
            " test_id, doctor_id, technician_id, " +
            " test_date, test_status, uploaded_by) " +
            "VALUES (?,?,?,?,?,?,?,?,?) RETURNING file_id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, draft.getPatient().getPatientNumber());
            ps.setBytes (2, ciphertext);
            ps.setString(3, draft.getOriginalFile().getName());
            ps.setInt   (4, testId);
            ps.setInt   (5, docId);
            ps.setInt   (6, techId);

            if (draft.getTestDate() != null)
                ps.setDate(7, Date.valueOf(draft.getTestDate()));
            else
                ps.setNull(7, Types.DATE);

            ps.setString(8, draft.getTestStatus());

            if (uploadedBy > 0) ps.setInt(9, uploadedBy);
            else                ps.setNull(9, Types.INTEGER);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        throw new Exception("INSERT into medical_files returned no file_id.");
    }

    /** Insert a new version with explicit version number and link to previous. */
    private static int insertFileVersioned(Connection conn, RecordDraft draft,
                                           byte[] ciphertext, int testId, int docId,
                                           int techId, int uploadedBy,
                                           int fileVersion, int previousFileId) throws Exception {
        final String sql =
            "INSERT INTO medical_files " +
            "(patient_number, encrypted_data, original_name, " +
            " test_id, doctor_id, technician_id, " +
            " test_date, test_status, uploaded_by, " +
            " file_version, previous_version_id) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?) RETURNING file_id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, draft.getPatient().getPatientNumber());
            ps.setBytes (2, ciphertext);
            ps.setString(3, draft.getOriginalFile().getName());
            ps.setInt   (4, testId);
            ps.setInt   (5, docId);
            ps.setInt   (6, techId);

            if (draft.getTestDate() != null)
                ps.setDate(7, Date.valueOf(draft.getTestDate()));
            else
                ps.setNull(7, Types.DATE);

            ps.setString(8, draft.getTestStatus());

            if (uploadedBy > 0) ps.setInt(9, uploadedBy);
            else                ps.setNull(9, Types.INTEGER);

            ps.setInt(10, fileVersion);
            ps.setInt(11, previousFileId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        throw new Exception("INSERT (versioned) into medical_files returned no file_id.");
    }

    private static void markViewed(int fileId) {
        updateStatus(fileId, "VIEWED");
    }

    /**
     * Overwrites the file with zeros then deletes it.
     * Called ONLY after a confirmed successful DB commit.
     *
     * If the OS refuses deletion (e.g. the file is locked), the zeroed
     * file is moved to the JVM temp directory and scheduled for deletion
     * on JVM exit. This prevents any readable plaintext from remaining at
     * the original path.
     */
    private static void secureDelete(File file) {
        try {
            long size = file.length();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] zeros = new byte[(int) Math.min(size, 65_536)];
                long done = 0;
                while (done < size) {
                    int chunk = (int) Math.min(zeros.length, size - done);
                    fos.write(zeros, 0, chunk);
                    done += chunk;
                }
                fos.flush();
            }
            if (!file.delete()) {
                File tmp = new File(System.getProperty("java.io.tmpdir"),
                        "smls_" + System.currentTimeMillis() + ".del");
                Files.move(file.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                tmp.deleteOnExit();
                System.err.println("[FileService] Could not delete original; moved to: " + tmp);
            }
        } catch (Exception e) {
            System.err.println("[FileService] secureDelete failed for " + file + ": " + e.getMessage());
        }
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private static FileRecord mapRow(ResultSet rs, boolean includeBlob) throws Exception {
        FileRecord r = new FileRecord();
        r.setFileId       (rs.getInt("file_id"));
        r.setPatientNumber(rs.getString("patient_number"));
        r.setPatientName  (rs.getString("patient_name"));
        if (includeBlob) r.setEncryptedData(rs.getBytes("encrypted_data"));
        r.setOriginalName  (rs.getString("original_name"));
        r.setTestType      (rs.getString("test_name"));
        r.setDoctorName    (rs.getString("doctor_name"));
        r.setTechnicianName(rs.getString("technician_name"));
        Date d = rs.getDate("test_date");
        if (d != null) r.setTestDate(d.toLocalDate());
        r.setTestStatus  (rs.getString("test_status"));
        r.setStatus      (rs.getString("status"));
        r.setUploadedBy  (rs.getInt("uploaded_by"));
        r.setUploaderName(rs.getString("uploader_name"));
        Timestamp ts = rs.getTimestamp("uploaded_at");
        if (ts != null) r.setUploadedAt(ts.toLocalDateTime());
        r.setFileVersion(rs.getInt("file_version"));
        int prevId = rs.getInt("previous_version_id");
        if (!rs.wasNull()) r.setPreviousVersionId(prevId);
        return r;
    }

    private FileService() {}
}
