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
 *
 * Passwords are hashed with PBKDF2-HMAC-SHA256:
 *   iterations : 100,000
 *   output     : 256-bit key
 *   salt       : 16 random bytes, per-user, stored Base64-encoded in the DB
 *
 * Two login entry points:
 *   login(username, password)  — traditional username+password (used by Admin)
 *   loginByRole(role, password) — role-first flow (used by LandingPage for
 *                                  Receptionist and Doctor cards)
 */
public final class AuthService {

    private static final int    ITERATIONS = 100_000;
    private static final int    KEY_BITS   = 256;
    private static final String ALGORITHM  = "PBKDF2WithHmacSHA256";

    // ── Public Login API ──────────────────────────────────────────────────────

    /**
     * Standard credential login.
     * Returns the User on success, null on bad credentials or DB error.
     * Only active accounts are accepted.
     */
    public static User login(String username, String password) {
        final String sql =
            "SELECT user_id, username, password_hash, salt, role, full_name " +
            "FROM users WHERE username = ? AND active = TRUE";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            if (!verify(password, rs.getString("salt"), rs.getString("password_hash")))
                return null;

            return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                Role.valueOf(rs.getString("role")),
                rs.getString("full_name")   // active=true implied by query filter
            );
        } catch (Exception e) {
            System.err.println("[AuthService] login error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Role-first login for the new LandingPage flow.
     *
     * The user selects their role card (Receptionist / Doctor), then
     * enters only a password. This method finds the active account with
     * that role and verifies the password.
     *
     * Assumption: one active account per role. If multiple active accounts
     * share the same role, the one with the lowest user_id is used
     * (deterministic but arbitrary — the multi-user case will be handled in
     * Command 3 with a username picker shown after role selection).
     *
     * Returns the User on success, null on bad password or no account found.
     */
    public static User loginByRole(Role role, String password) {
        final String sql =
            "SELECT user_id, username, password_hash, salt, full_name " +
            "FROM users WHERE role = ? AND active = TRUE ORDER BY user_id LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            if (!verify(password, rs.getString("salt"), rs.getString("password_hash")))
                return null;

            return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                role,
                rs.getString("full_name")
            );
        } catch (Exception e) {
            System.err.println("[AuthService] loginByRole error: " + e.getMessage());
            return null;
        }
    }

    // ── Account Management ────────────────────────────────────────────────────

    /**
     * Creates a new user with a freshly hashed password.
     * Throws on duplicate username or DB error.
     * Called by AdminService.createUser() and seedDefaultUsersIfEmpty().
     */
    public static void createUser(String username, String password,
                                  Role role, String fullName) throws Exception {
        String salt = generateSalt();
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
     * Replaces a user's password with a newly hashed one.
     * Called by AdminService.resetPassword().
     * Throws if the user_id does not exist.
     */
    public static void updatePassword(int userId, String newPassword) throws Exception {
        String salt = generateSalt();
        String hash = hash(newPassword, salt);

        try (Connection conn = DatabaseConnection.connect();
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
     *
     * Default credentials:
     *   receptionist / Recep@1234  [RECEPTIONIST]
     *   doctor       / Doctor@1234 [DOCTOR]
     *   admin        / Admin@1234  [ADMIN]
     *
     * These should be changed by the Admin on first login.
     */
    public static void seedDefaultUsersIfEmpty() {
        try (Connection conn = DatabaseConnection.connect();
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

    // ── Private PBKDF2 Helpers ────────────────────────────────────────────────

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
