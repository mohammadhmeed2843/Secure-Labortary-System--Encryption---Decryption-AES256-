package javafxapplication7.service;

import javafxapplication7.DatabaseConnection;
import javafxapplication7.model.FileRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * File version history and recovery.
 *
 * Version chain: each superseded file keeps its row in medical_files with
 * status = ARCHIVED and previous_version_id pointing to its predecessor.
 * The active version has the highest file_version for that patient+test
 * combination and status != ARCHIVED (typically READY or VIEWED).
 *
 * restoreVersion() flips the old version back to READY and archives the
 * currently active version — it does NOT copy or move any encrypted bytes,
 * because all versions already live in medical_files.
 */
public final class HistoryService {

    /**
     * Returns the version chain for a file, starting from the given fileId
     * and walking backwards via previous_version_id until there are no more
     * predecessors. The list is ordered newest-first (the given file is [0]).
     */
    public static List<FileRecord> getVersionChain(int fileId) throws Exception {
        List<FileRecord> chain = new ArrayList<>();
        Integer current = fileId;
        while (current != null) {
            FileRecord r = loadShallow(current);
            if (r == null) break;
            chain.add(r);
            current = r.getPreviousVersionId();
        }
        return chain;
    }

    /**
     * Restores an older version of a file.
     *
     * oldFileId    — the version to make active again (set to READY)
     * currentFileId — the currently active version (set to ARCHIVED)
     *
     * Both operations run in a single transaction.
     */
    public static void restoreVersion(int oldFileId, int currentFileId) throws Exception {
        try (Connection conn = DatabaseConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                setStatus(conn, currentFileId, "ARCHIVED");
                setStatus(conn, oldFileId,     "READY");
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * All archived files across all patients, ordered by upload date descending.
     * Used by the Admin recovery panel.
     */
    public static List<FileRecord> listArchivedFiles() throws Exception {
        final String sql =
            "SELECT mf.file_id, mf.patient_number, " +
            "       p.first_name || ' ' || p.last_name AS patient_name, " +
            "       mf.original_name, t.test_name, d.doctor_name, tech.technician_name, " +
            "       mf.test_date, mf.test_status, mf.status, " +
            "       mf.uploaded_by, u.full_name AS uploader_name, mf.uploaded_at, " +
            "       mf.file_version, mf.previous_version_id " +
            "FROM medical_files mf " +
            "JOIN patients p         ON p.patient_number   = mf.patient_number " +
            "LEFT JOIN tests        t    ON t.test_id          = mf.test_id " +
            "LEFT JOIN doctors      d    ON d.doctor_id        = mf.doctor_id " +
            "LEFT JOIN technicians  tech ON tech.technician_id = mf.technician_id " +
            "LEFT JOIN users        u    ON u.user_id          = mf.uploaded_by " +
            "WHERE mf.status = 'ARCHIVED' " +
            "ORDER BY mf.uploaded_at DESC";

        List<FileRecord> out = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static FileRecord loadShallow(int fileId) throws Exception {
        final String sql =
            "SELECT mf.file_id, mf.patient_number, " +
            "       p.first_name || ' ' || p.last_name AS patient_name, " +
            "       mf.original_name, t.test_name, d.doctor_name, tech.technician_name, " +
            "       mf.test_date, mf.test_status, mf.status, " +
            "       mf.uploaded_by, u.full_name AS uploader_name, mf.uploaded_at, " +
            "       mf.file_version, mf.previous_version_id " +
            "FROM medical_files mf " +
            "JOIN patients p         ON p.patient_number   = mf.patient_number " +
            "LEFT JOIN tests        t    ON t.test_id          = mf.test_id " +
            "LEFT JOIN doctors      d    ON d.doctor_id        = mf.doctor_id " +
            "LEFT JOIN technicians  tech ON tech.technician_id = mf.technician_id " +
            "LEFT JOIN users        u    ON u.user_id          = mf.uploaded_by " +
            "WHERE mf.file_id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    private static void setStatus(Connection conn, int fileId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE medical_files SET status = ? WHERE file_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, fileId);
            ps.executeUpdate();
        }
    }

    private static FileRecord mapRow(ResultSet rs) throws Exception {
        FileRecord r = new FileRecord();
        r.setFileId       (rs.getInt("file_id"));
        r.setPatientNumber(rs.getString("patient_number"));
        r.setPatientName  (rs.getString("patient_name"));
        r.setOriginalName (rs.getString("original_name"));
        r.setTestType     (rs.getString("test_name"));
        r.setDoctorName   (rs.getString("doctor_name"));
        r.setTechnicianName(rs.getString("technician_name"));
        java.sql.Date d = rs.getDate("test_date");
        if (d != null) r.setTestDate(d.toLocalDate());
        r.setTestStatus  (rs.getString("test_status"));
        r.setStatus      (rs.getString("status"));
        r.setUploadedBy  (rs.getInt("uploaded_by"));
        r.setUploaderName(rs.getString("uploader_name"));
        java.sql.Timestamp ts = rs.getTimestamp("uploaded_at");
        if (ts != null) r.setUploadedAt(ts.toLocalDateTime());
        r.setFileVersion(rs.getInt("file_version"));
        int prevId = rs.getInt("previous_version_id");
        if (!rs.wasNull()) r.setPreviousVersionId(prevId);
        return r;
    }

    private HistoryService() {}
}
