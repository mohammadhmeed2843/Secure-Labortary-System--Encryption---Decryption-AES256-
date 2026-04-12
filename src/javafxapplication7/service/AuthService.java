package javafxapplication7.service;

import javafxapplication7.DatabaseConnection;
import javafxapplication7.model.Role;
import javafxapplication7.model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

/**
 * Authentication service.
 * Passwords are hashed with PBKDF2-HMAC-SHA256 (100 000 iterations, 256-bit output)
 * with a per-user random salt stored alongside the hash.
 */
public final class AuthService {

    private static final int    ITERATIONS = 100_000;
    private static final int    KEY_BITS   = 256;
    private static final String ALGORITHM  = "PBKDF2WithHmacSHA256";

    /**
     * Validates credentials against the users table.
     * Returns the User on success, null on failure (bad credentials or DB error).
     */
    public static User login(String username, String password) {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT user_id, username, password_hash, salt, role, full_name " +
                 "FROM users WHERE username = ? AND active = TRUE")) {

            ps.setString(1, username.trim());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            String storedHash = rs.getString("password_hash");
            String storedSalt = rs.getString("salt");

            if (!verify(password, storedSalt, storedHash)) return null;

            Role role = Role.valueOf(rs.getString("role"));
            return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                role,
                rs.getString("full_name")
            );

        } catch (Exception e) {
            System.err.println("AuthService.login error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a new user with a hashed password.
     * Throws on duplicate username or DB error.
     */
    public static void createUser(String username, String password,
                                  Role role, String fullName) throws Exception {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        String salt = Base64.getEncoder().encodeToString(saltBytes);
        String hash = hash(password, salt);

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO users (username, password_hash, salt, role, full_name) " +
                 "VALUES (?,?,?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.setString(4, role.name());
            ps.setString(5, fullName);
            ps.executeUpdate();
        }
    }

    /**
     * Inserts three default users on first run if the users table is empty.
     * Default credentials (change via admin UI in Command 3):
     *   admin       / Admin@1234    [ADMIN]
     *   technician  / Tech@1234     [TECHNICIAN]
     *   doctor      / Doctor@1234   [DOCTOR]
     */
    public static void seedDefaultUsersIfEmpty() {
        try (Connection conn = DatabaseConnection.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) == 0) {
                createUser("admin",      "Admin@1234",  Role.ADMIN,      "System Administrator");
                createUser("technician", "Tech@1234",   Role.TECHNICIAN, "Lab Technician");
                createUser("doctor",     "Doctor@1234", Role.DOCTOR,     "Dr. Default");
                System.out.println("[AuthService] Default users seeded.");
            }
        } catch (Exception e) {
            System.err.println("[AuthService] Could not seed default users: " + e.getMessage());
        }
    }

    // ── Private crypto helpers ────────────────────────────────────────────────

    private static String hash(String password, String base64Salt) throws Exception {
        byte[] salt = Base64.getDecoder().decode(base64Salt);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        byte[] hash = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verify(String password, String base64Salt, String expectedHash) {
        try { return hash(password, base64Salt).equals(expectedHash); }
        catch (Exception e) { return false; }
    }

    private AuthService() {}
}
