package javafxapplication7.services;

import javafxapplication7.data.DB;
import javafxapplication7.models.Role;
import javafxapplication7.models.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

/**
 * Authentication service.
 *
 * Passwords are hashed with PBKDF2-HMAC-SHA256 (100,000 iterations, 256-bit key).
 * Two login entry points:
 *   login(username, password)   — standard credential login (Admin)
 *   loginByRole(role, password) — role-first flow (Receptionist / Doctor cards)
 */
public final class AuthService {

    private static final int    ITERATIONS = 100_000;
    private static final int    KEY_BITS   = 256;
    private static final String ALGORITHM  = "PBKDF2WithHmacSHA256";

    // ── Login ─────────────────────────────────────────────────────────────────

    public static User login(String username, String password) {
        final String sql =
            "SELECT user_id, username, password_hash, salt, role, full_name " +
            "FROM users WHERE username = ? AND active = TRUE";
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            if (!verify(password, rs.getString("salt"), rs.getString("password_hash")))
                return null;
            return new User(rs.getInt("user_id"), rs.getString("username"),
                    Role.valueOf(rs.getString("role")), rs.getString("full_name"));
        } catch (Exception e) {
            System.err.println("[AuthService] login error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Role-first login for the role-card entry flow.
     * Finds the active account for the given role and verifies the password.
     */
    public static User loginByRole(Role role, String password) {
        final String sql =
            "SELECT user_id, username, password_hash, salt, full_name " +
            "FROM users WHERE role = ? AND active = TRUE ORDER BY user_id LIMIT 1";
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            if (!verify(password, rs.getString("salt"), rs.getString("password_hash")))
                return null;
            return new User(rs.getInt("user_id"), rs.getString("username"),
                    role, rs.getString("full_name"));
        } catch (Exception e) {
            System.err.println("[AuthService] loginByRole error: " + e.getMessage());
            return null;
        }
    }

    // ── Account management ────────────────────────────────────────────────────

    public static void createUser(String username, String password,
                                  Role role, String fullName) throws Exception {
        String salt = generateSalt();
        String hash = hash(password, salt);
        try (Connection conn = DB.connect();
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

    public static void updatePassword(int userId, String newPassword) throws Exception {
        String salt = generateSalt();
        String hash = hash(newPassword, salt);
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?")) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setInt(3, userId);
            if (ps.executeUpdate() == 0)
                throw new Exception("User not found: user_id=" + userId);
        }
    }

    /**
     * Seeds three default accounts on first launch if the users table is empty.
     * Receptionist: Recep@1234 / Doctor: Doctor@1234 / Admin: Admin@1234
     */
    public static void seedDefaultUsersIfEmpty() {
        try (Connection conn = DB.connect();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) == 0) {
                createUser("receptionist", "Recep@1234",  Role.RECEPTIONIST, "Lab Receptionist");
                createUser("doctor",       "Doctor@1234", Role.DOCTOR,       "Dr. Default");
                createUser("admin",        "Admin@1234",  Role.ADMIN,        "System Administrator");
                System.out.println("[AuthService] Default users seeded.");
            }
        } catch (Exception e) {
            System.err.println("[AuthService] Could not seed default users: " + e.getMessage());
        }
    }

    // ── PBKDF2 helpers ────────────────────────────────────────────────────────

    private static String generateSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String hash(String password, String base64Salt) throws Exception {
        byte[]     salt = Base64.getDecoder().decode(base64Salt);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        byte[]     raw  = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(raw);
    }

    private static boolean verify(String password, String base64Salt, String expectedHash) {
        try { return hash(password, base64Salt).equals(expectedHash); }
        catch (Exception e) { return false; }
    }

    private AuthService() {}
}
