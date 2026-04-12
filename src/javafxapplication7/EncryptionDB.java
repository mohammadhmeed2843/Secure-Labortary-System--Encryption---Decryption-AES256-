package javafxapplication7;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Standalone utility for verifying the database connection and
 * confirming that all required tables exist.
 *
 * <p>Run this class's {@code main} method directly to validate the
 * environment before starting the full application.</p>
 */
public class EncryptionDB {

    /** Tables that must exist for the application to function. */
    private static final String[] REQUIRED_TABLES = {
        "patients", "tests", "doctors", "technicians", "test_records"
    };

    public static void main(String[] args) {
        System.out.println("=== Database Health Check ===");
        try (Connection conn = DatabaseConnection.connect()) {
            System.out.println("✅ Connected to PostgreSQL!");

            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("   Server: " + meta.getDatabaseProductVersion());
            System.out.println();

            System.out.println("Checking required tables:");
            boolean allOk = true;
            for (String table : REQUIRED_TABLES) {
                try (ResultSet rs = meta.getTables(null, "public", table, new String[]{"TABLE"})) {
                    if (rs.next()) {
                        System.out.println("  ✅ " + table);
                    } else {
                        System.out.println("  ❌ " + table + "  ← MISSING — run setup_database.sql");
                        allOk = false;
                    }
                }
            }

            System.out.println();
            if (allOk) {
                System.out.println("✅ All checks passed. Application is ready to run.");
            } else {
                System.out.println("❌ Some tables are missing. Run setup_database.sql and retry.");
            }

        } catch (Exception e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
