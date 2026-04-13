package javafxapplication7;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Standalone database health-check utility.
 *
 * Run this class's main() directly to verify that the PostgreSQL database
 * is reachable and all required tables exist before starting the application.
 *
 * Updated for Phase 3 schema (Command 2):
 *   - Removed: test_records (dropped in migrate_v3.sql)
 *   - Added  : users, medical_files, file_keys (Phase 2 tables)
 */
public class EncryptionDB {

    /** All tables that must exist for the application to function correctly. */
    private static final String[] REQUIRED_TABLES = {
        "patients",
        "tests",
        "doctors",
        "technicians",
        "users",
        "medical_files",
        "file_keys"
    };

    public static void main(String[] args) {
        System.out.println("=== Secure Medical Lab System — Database Health Check ===");
        System.out.println();

        try (Connection conn = DatabaseConnection.connect()) {
            System.out.println("✅ Connected to PostgreSQL!");

            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("   Server  : " + meta.getDatabaseProductVersion());
            System.out.println("   Database: " + conn.getCatalog());
            System.out.println();

            System.out.println("Checking required tables:");
            boolean allOk = true;
            for (String table : REQUIRED_TABLES) {
                try (ResultSet rs = meta.getTables(null, "public", table, new String[]{"TABLE"})) {
                    if (rs.next()) {
                        System.out.println("  ✅ " + table);
                    } else {
                        System.out.println("  ❌ " + table + "  ← MISSING");
                        allOk = false;
                    }
                }
            }

            System.out.println();
            if (allOk) {
                System.out.println("✅ All checks passed. Run migrate_v2.sql then migrate_v3.sql if any tables are missing.");
            } else {
                System.out.println("❌ Some tables are missing.");
                System.out.println("   Run: psql -U postgres -d pdfencryptionfolder -f migrate_v2.sql");
                System.out.println("   Then: psql -U postgres -d pdfencryptionfolder -f migrate_v3.sql");
            }

        } catch (Exception e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
