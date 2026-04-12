package javafxapplication7.service;

import javafxapplication7.DatabaseConnection;
import javafxapplication7.model.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access layer for the patients table.
 * Upsert accepts a Connection to participate in the caller's transaction.
 */
public final class PatientService {

    /**
     * Inserts a new patient or updates name/dob if the patient_number already exists.
     * Called inside FileService's upload transaction.
     */
    public static void upsert(Connection conn, Patient patient) throws Exception {
        String sql =
            "INSERT INTO patients (patient_number, first_name, last_name, dob) VALUES (?,?,?,?) " +
            "ON CONFLICT (patient_number) DO UPDATE SET " +
            "first_name = EXCLUDED.first_name, " +
            "last_name  = EXCLUDED.last_name,  " +
            "dob        = EXCLUDED.dob";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patient.getPatientNumber());
            ps.setString(2, patient.getFirstName());
            ps.setString(3, patient.getLastName());
            ps.setDate  (4, Date.valueOf(patient.getDob()));
            ps.executeUpdate();
        }
    }

    /** Returns all patients ordered by name. */
    public static List<Patient> listAll() throws Exception {
        List<Patient> out = new ArrayList<>();
        String sql = "SELECT patient_number, first_name, last_name, dob " +
                     "FROM patients ORDER BY last_name, first_name";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Patient(
                    rs.getString("patient_number"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getDate("dob").toLocalDate()
                ));
            }
        }
        return out;
    }

    private PatientService() {}
}
