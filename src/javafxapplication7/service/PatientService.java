package javafxapplication7.service;

import javafxapplication7.DatabaseConnection;
import javafxapplication7.model.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access layer for the patients table.
 *
 * Design notes:
 *  - upsert() participates in the caller's transaction (accepts Connection).
 *  - All read methods open their own connection via try-with-resources.
 *  - Null guards on dob prevent NPE when a patient record has no date of birth.
 */
public final class PatientService {

    /**
     * Inserts a new patient or updates name/dob when the patient_number already
     * exists. Called inside FileService's upload transaction.
     *
     * Guard: if dob is null the column is set to SQL NULL rather than throwing.
     */
    public static void upsert(Connection conn, Patient patient) throws Exception {
        final String sql =
            "INSERT INTO patients (patient_number, first_name, last_name, dob) " +
            "VALUES (?,?,?,?) " +
            "ON CONFLICT (patient_number) DO UPDATE SET " +
            "  first_name = EXCLUDED.first_name, " +
            "  last_name  = EXCLUDED.last_name,  " +
            "  dob        = EXCLUDED.dob";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patient.getPatientNumber());
            ps.setString(2, patient.getFirstName());
            ps.setString(3, patient.getLastName());

            // Guard: dob may be null for patients whose birth date is unknown
            if (patient.getDob() != null) {
                ps.setDate(4, Date.valueOf(patient.getDob()));
            } else {
                ps.setNull(4, Types.DATE);
            }

            ps.executeUpdate();
        }
    }

    /**
     * Returns all patients ordered by last name, first name.
     * Used by PatientFilesController (doctor search) and UploadTestController
     * (patient look-up).
     */
    public static List<Patient> listAll() throws Exception {
        List<Patient> out = new ArrayList<>();
        final String sql =
            "SELECT patient_number, first_name, last_name, dob " +
            "FROM patients ORDER BY last_name, first_name";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Date d = rs.getDate("dob");
                out.add(new Patient(
                    rs.getString("patient_number"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    d != null ? d.toLocalDate() : null   // null-safe
                ));
            }
        }
        return out;
    }

    /**
     * Looks up a single patient by patient number.
     * Returns null if no matching patient exists.
     */
    public static Patient findByNumber(String patientNumber) throws Exception {
        final String sql =
            "SELECT patient_number, first_name, last_name, dob " +
            "FROM patients WHERE patient_number = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patientNumber.trim());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            Date d = rs.getDate("dob");
            return new Patient(
                rs.getString("patient_number"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                d != null ? d.toLocalDate() : null
            );
        }
    }

    /**
     * Searches patients whose full name or patient number contains the query
     * string (case-insensitive). Returns up to {@code limit} results.
     * Used by the patient search field in PatientFilesController.
     */
    public static List<Patient> search(String query, int limit) throws Exception {
        List<Patient> out = new ArrayList<>();
        final String sql =
            "SELECT patient_number, first_name, last_name, dob " +
            "FROM patients " +
            "WHERE LOWER(first_name || ' ' || last_name) LIKE ? " +
            "   OR LOWER(patient_number) LIKE ? " +
            "ORDER BY last_name, first_name " +
            "LIMIT ?";

        String pattern = "%" + query.toLowerCase().trim() + "%";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date d = rs.getDate("dob");
                out.add(new Patient(
                    rs.getString("patient_number"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    d != null ? d.toLocalDate() : null
                ));
            }
        }
        return out;
    }

    private PatientService() {}
}
