package javafxapplication7.service;

import javafxapplication7.DatabaseConnection;
import javafxapplication7.model.Role;
import javafxapplication7.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Admin-only operations.
 *
 * Responsibilities:
 *  - User account management (list, activate/deactivate, reset password)
 *  - System-wide statistics for the admin dashboard
 *  - Placeholder structure for future audit log access
 *
 * All methods assume the caller has already verified ADMIN role via
 * PermissionService.canManageUsers(). This class enforces no access control
 * internally — that is the controller's responsibility.
 *
 * Password hashing is delegated entirely to AuthService so that the
 * PBKDF2 logic remains in exactly one place.
 */
public final class AdminService {

    // ── User Account Management ───────────────────────────────────────────────

    /**
     * Returns all user accounts (active and inactive), ordered by role then name.
     * Inactive users are shown so the admin can re-activate them.
     */
    public static List<User> listAllUsers() throws Exception {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, role, full_name, active " +
                     "FROM users ORDER BY role, full_name";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    Role.valueOf(rs.getString("role")),
                    rs.getString("full_name"),
                    rs.getBoolean("active")
                ));
            }
        }
        return users;
    }

    /**
     * Activates or deactivates a user account.
     * A deactivated user cannot log in (AuthService filters active=TRUE).
     */
    public static void setUserActive(int userId, boolean active) throws Exception {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE users SET active = ? WHERE user_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            if (ps.executeUpdate() == 0)
                throw new Exception("User not found: user_id=" + userId);
        }
    }

    /**
     * Resets a user's password.
     * Delegates to AuthService so PBKDF2 logic stays in one place.
     */
    public static void resetPassword(int userId, String newPassword) throws Exception {
        if (newPassword == null || newPassword.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        AuthService.updatePassword(userId, newPassword);
    }

    /**
     * Creates a new user account.
     * Delegates to AuthService for PBKDF2 hashing and DB insert.
     */
    public static void createUser(String username, String password,
                                  Role role, String fullName) throws Exception {
        AuthService.createUser(username, password, role, fullName);
    }

    // ── System Statistics ─────────────────────────────────────────────────────

    /**
     * Returns a snapshot of system-wide statistics.
     * Uses a single connection for all queries (efficient).
     *
     * Keys returned:
     *   totalRecords, readyRecords, viewedRecords, archivedRecords,
     *   totalPatients, totalActiveUsers
     */
    public static Map<String, Integer> getSystemStats() throws Exception {
        Map<String, Integer> stats = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.connect()) {
            stats.put("totalRecords",    count(conn, "SELECT COUNT(*) FROM medical_files"));
            stats.put("readyRecords",    count(conn, "SELECT COUNT(*) FROM medical_files WHERE status='READY'"));
            stats.put("viewedRecords",   count(conn, "SELECT COUNT(*) FROM medical_files WHERE status='VIEWED'"));
            stats.put("archivedRecords", count(conn, "SELECT COUNT(*) FROM medical_files WHERE status='ARCHIVED'"));
            stats.put("totalPatients",   count(conn, "SELECT COUNT(*) FROM patients"));
            stats.put("totalActiveUsers",count(conn, "SELECT COUNT(*) FROM users WHERE active=TRUE"));
        }
        return stats;
    }

    // ── Future: Audit Log ─────────────────────────────────────────────────────
    //
    // When an audit_log table is added in migrate_v4.sql (Command 3 or 4),
    // add the following method:
    //
    // public static List<AuditEntry> getRecentAuditLog(int limit) throws Exception {
    //     String sql = "SELECT ... FROM audit_log ORDER BY created_at DESC LIMIT ?";
    //     ...
    // }
    //
    // The AuditEntry model and audit_log table schema will be defined then.
    // Controllers should call this method rather than querying directly.

    // ── Private Helpers ───────────────────────────────────────────────────────

    private static int count(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private AdminService() {}
}
