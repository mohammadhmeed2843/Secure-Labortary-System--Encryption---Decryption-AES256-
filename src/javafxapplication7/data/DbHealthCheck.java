package javafxapplication7.data;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Standalone database health-check utility.
 *
 * Run main() directly to verify that the PostgreSQL database is reachable
 * and all required tables exist before starting the application.
 */
public final class DbHealthCheck {

    private static final String[] REQUIRED_TABLES = {
        "patients", "tests", "doctors", "technicians",
        "users", "medical_files", "file_keys"
    };

    public static void main(String[] args) {
        System.out.println("=== Secure Medical Lab System — Database Health Check ===\n");

        try (Connection conn = DB.connect()) {
            System.out.println("Connected to PostgreSQL!");

            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("   Server  : " + meta.getDatabaseProductVersion());
            System.out.println("   Database: " + conn.getCatalog() + "\n");

            System.out.println("Checking required tables:");
            boolean allOk = true;
            for (String table : REQUIRED_TABLES) {
                try (ResultSet rs = meta.getTables(null, "public", table, new String[]{"TABLE"})) {
                    if (rs.next()) {
                        System.out.println("  OK  " + table);
                    } else {
                        System.out.println("  MISSING  " + table);
                        allOk = false;
                    }
                }
            }

            System.out.println();
            if (allOk) {
                System.out.println("All checks passed.");
            } else {
                System.out.println("Some tables are missing. Run migrate_v2.sql then migrate_v3.sql.");
            }

        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private DbHealthCheck() {}
}
