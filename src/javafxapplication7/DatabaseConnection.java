package javafxapplication7;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Single point of access for obtaining a PostgreSQL JDBC connection.
 *
 * Target database : pdfencryptionfolder @ localhost:5432
 * To change credentials, update USER and PASSWORD below.
 *
 * Every caller is responsible for closing the connection, preferably
 * via try-with-resources:
 *
 *   try (Connection conn = DatabaseConnection.connect()) {
 *       // use conn
 *   }
 *
 * Note: this class opens a new physical connection on every call.
 * A connection pool (HikariCP or similar) should be introduced if the
 * application scales beyond a handful of concurrent background tasks.
 */
public class DatabaseConnection {

    // ── Credentials — update to match your PostgreSQL installation ──────────
    private static final String USER     = "postgres";
    private static final String PASSWORD = "0000";
    // ────────────────────────────────────────────────────────────────────────

    private static final String URL =
            "jdbc:postgresql://localhost:5432/pdfencryptionfolder";

    /**
     * Opens and returns a new Connection to the PostgreSQL database.
     *
     * @return an open, auto-commit-enabled connection
     * @throws SQLException if the driver is missing or the connection fails
     */
    public static Connection connect() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "PostgreSQL JDBC driver not found. " +
                "Ensure postgresql-*.jar is on the classpath.", e);
        }

        Properties props = new Properties();
        props.setProperty("user",            USER);
        props.setProperty("password",        PASSWORD);
        props.setProperty("connectTimeout",  "10");
        props.setProperty("socketTimeout",   "30");
        props.setProperty("ApplicationName", "SecureMedicalLabSystem");

        return DriverManager.getConnection(URL, props);
    }
}
