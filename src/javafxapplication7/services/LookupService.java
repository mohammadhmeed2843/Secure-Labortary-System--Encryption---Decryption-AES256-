package javafxapplication7.services;

import javafxapplication7.data.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and manages reference/lookup data (tests, doctors, technicians).
 * Transactional insert-or-get methods accept a Connection to participate in the caller's transaction.
 */
public final class LookupService {

    public static List<String> getTestTypes() throws Exception {
        return loadColumn("SELECT test_name FROM tests ORDER BY test_name");
    }

    public static List<String> getDoctors() throws Exception {
        return loadColumn("SELECT doctor_name FROM doctors ORDER BY doctor_name");
    }

    public static List<String> getTechnicians() throws Exception {
        return loadColumn("SELECT technician_name FROM technicians ORDER BY technician_name");
    }

    public static int getOrCreateTest(Connection conn, String name) throws Exception {
        return getOrCreate(conn, "tests", "test_id", "test_name", name);
    }

    public static int getOrCreateDoctor(Connection conn, String name) throws Exception {
        return getOrCreate(conn, "doctors", "doctor_id", "doctor_name", name);
    }

    public static int getOrCreateTechnician(Connection conn, String name) throws Exception {
        return getOrCreate(conn, "technicians", "technician_id", "technician_name", name);
    }

    private static List<String> loadColumn(String sql) throws Exception {
        List<String> out = new ArrayList<>();
        try (Connection conn = DB.connect();
             Statement st   = conn.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    private static int getOrCreate(Connection conn, String table,
                                   String idCol, String nameCol, String value) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT " + idCol + " FROM " + table + " WHERE " + nameCol + " = ?")) {
            ps.setString(1, value);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO " + table + " (" + nameCol + ") VALUES (?) RETURNING " + idCol)) {
            ps.setString(1, value);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        throw new Exception("getOrCreate failed for " + table + "." + nameCol + "=" + value);
    }

    private LookupService() {}
}
