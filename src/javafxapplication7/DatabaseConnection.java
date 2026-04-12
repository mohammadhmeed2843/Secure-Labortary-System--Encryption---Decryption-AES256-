package javafxapplication7;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Provides a single point of access for obtaining a PostgreSQL database
 * connection.
 *
 * <p>Connection targets the {@code pdfencryptionfolder} database running
 * on localhost:5432. To change credentials, update {@code USER} and
 * {@code PASSWORD} below.</p>
 *
 * <p>Every caller is responsible for closing the connection, preferably
 * via try-with-resources:</p>
 * <pre>
 *   try (Connection conn = DatabaseConnection.connect()) {
 *       // use conn
 *   }
 * </pre>
 */
public class DatabaseConnection {

    // ── Change only these two values to match your PostgreSQL credentials ──
    private static final String USER     = "postgres";
    private static final String PASSWORD = "0000";
    // ───────────────────────────────────────────────────────────────────────

    private static final String URL =
            "jdbc:postgresql://localhost:5432/pdfencryptionfolder";

    /**
     * Opens and returns a new {@link Connection} to the PostgreSQL database.
     *
     * @return an open, auto-commit-enabled connection
     * @throws SQLException if the driver is missing or the connection fails
     */
    public static Connection connect() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found. " +
                    "Ensure postgresql-*.jar is on the classpath.", e);
        }

        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        props.setProperty("connectTimeout", "10");       // seconds
        props.setProperty("socketTimeout",  "30");       // seconds
        props.setProperty("ApplicationName", "SecureMedicalFileSystem");

        return DriverManager.getConnection(URL, props);
    }

    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.connect()) {
            if (conn != null) {
                System.out.println("✅ Database connection successful!");
            } else {
                System.out.println("❌ Database connection failed!");
            }
        } catch (SQLException e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
