package javafxapplication7.service;

import javafxapplication7.DatabaseConnection;
import javafxapplication7.model.AuditEntry;
import javafxapplication7.session.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes and reads the audit_log table.
 *
 * Action constants are defined here so every caller spells them the same way.
 * logCurrent() is the convenient variant — it picks up userId + username from
 * the active Session, so controllers do not need to pass user details manually.
 */
public final class AuditService {

    // ── Action constants ──────────────────────────────────────────────────────
    public static final String LOGIN           = "LOGIN";
    public static final String LOGOUT          = "LOGOUT";
    public static final String UPLOAD          = "UPLOAD";
    public static final String UPDATE_FILE     = "UPDATE_FILE";
    public static final String VIEW            = "VIEW";
    public static final String EXPORT          = "EXPORT";
    public static final String ARCHIVE         = "ARCHIVE";
    public static final String RESTORE_FILE    = "RESTORE_FILE";
    public static final String RESET_PASSWORD  = "RESET_PASSWORD";
    public static final String DEACTIVATE_USER = "DEACTIVATE_USER";
    public static final String ACTIVATE_USER   = "ACTIVATE_USER";
    public static final String CREATE_USER     = "CREATE_USER";

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Log an action for the currently logged-in user.
     * Silent on failure — audit logging must never crash the main workflow.
     */
    public static void logCurrent(String action, String targetType,
                                  String targetId, String details) {
        if (!Session.isLoggedIn()) {
            log(null, null, action, targetType, targetId, details);
            return;
        }
        log(Session.getUser().getUserId(),
            Session.getUser().getUsername(),
            action, targetType, targetId, details);
    }

    /**
     * Log an action for an explicit userId / username.
     * Use this when the action concerns a different user than the current one
     * (e.g., admin resetting another user's password).
     */
    public static void log(Integer userId, String username,
                           String action, String targetType,
                           String targetId, String details) {
        final String sql =
            "INSERT INTO audit_log (user_id, username, action, target_type, target_id, details) " +
            "VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId);
            else                ps.setNull(1, Types.INTEGER);
            ps.setString(2, username);
            ps.setString(3, action);
            ps.setString(4, targetType);
            ps.setString(5, targetId);
            ps.setString(6, details);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[AuditService] log() failed: " + e.getMessage());
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Most recent N audit entries across all users. */
    public static List<AuditEntry> getRecentLogs(int limit) {
        return query(
            "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?",
            ps -> ps.setInt(1, limit));
    }

    /** Most recent N entries for a specific user. */
    public static List<AuditEntry> getLogsForUser(int userId, int limit) {
        return query(
            "SELECT * FROM audit_log WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
            ps -> { ps.setInt(1, userId); ps.setInt(2, limit); });
    }

    /** All entries whose target_id = fileId and target_type = 'file'. */
    public static List<AuditEntry> getLogsForFile(int fileId) {
        return query(
            "SELECT * FROM audit_log WHERE target_type = 'file' AND target_id = ? " +
            "ORDER BY created_at DESC",
            ps -> ps.setString(1, String.valueOf(fileId)));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private static List<AuditEntry> query(String sql, Binder binder) {
        List<AuditEntry> out = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(mapRow(rs));
        } catch (Exception e) {
            System.err.println("[AuditService] query failed: " + e.getMessage());
        }
        return out;
    }

    private static AuditEntry mapRow(ResultSet rs) throws SQLException {
        AuditEntry e = new AuditEntry();
        e.setLogId(rs.getInt("log_id"));
        int uid = rs.getInt("user_id");
        if (!rs.wasNull()) e.setUserId(uid);
        e.setUsername(rs.getString("username"));
        e.setAction(rs.getString("action"));
        e.setTargetType(rs.getString("target_type"));
        e.setTargetId(rs.getString("target_id"));
        e.setDetails(rs.getString("details"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) e.setCreatedAt(ts.toLocalDateTime());
        return e;
    }

    private AuditService() {}
}
