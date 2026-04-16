package javafxapplication7.data;

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
 *   try (Connection conn = DB.connect()) { ... }
 *
 * Note: this class opens a new physical connection on every call.
 * A connection pool (HikariCP or similar) should be introduced if the
 * application scales beyond a handful of concurrent background tasks.
 */
public final class DB {

    private static final String USER     = "postgres";
    private static final String PASSWORD = "0000";
    private static final String URL =
            "jdbc:postgresql://localhost:5432/pdfencryptionfolder";

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

    private DB() {}
}
