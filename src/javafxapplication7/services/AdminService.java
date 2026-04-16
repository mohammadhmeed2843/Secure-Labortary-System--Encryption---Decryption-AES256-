package javafxapplication7.services;

import javafxapplication7.data.DB;
import javafxapplication7.models.Role;
import javafxapplication7.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Admin-only operations: user account management and system statistics.
 * Assumes the caller has already verified ADMIN role via PermissionService.
 * Password hashing is delegated to AuthService so PBKDF2 logic stays in one place.
 */
public final class AdminService {

    public static List<User> listAllUsers() throws Exception {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, role, full_name, active " +
                     "FROM users ORDER BY role, full_name";
        try (Connection conn = DB.connect();
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

    public static void setUserActive(int userId, boolean active) throws Exception {
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE users SET active = ? WHERE user_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            if (ps.executeUpdate() == 0)
                throw new Exception("User not found: user_id=" + userId);
        }
    }

    public static void resetPassword(int userId, String newPassword) throws Exception {
        if (newPassword == null || newPassword.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        AuthService.updatePassword(userId, newPassword);
    }

    public static void createUser(String username, String password,
                                  Role role, String fullName) throws Exception {
        AuthService.createUser(username, password, role, fullName);
    }

    public static Map<String, Integer> getSystemStats() throws Exception {
        Map<String, Integer> stats = new LinkedHashMap<>();
        try (Connection conn = DB.connect()) {
            stats.put("totalRecords",    count(conn, "SELECT COUNT(*) FROM medical_files"));
            stats.put("readyRecords",    count(conn, "SELECT COUNT(*) FROM medical_files WHERE status='READY'"));
            stats.put("viewedRecords",   count(conn, "SELECT COUNT(*) FROM medical_files WHERE status='VIEWED'"));
            stats.put("archivedRecords", count(conn, "SELECT COUNT(*) FROM medical_files WHERE status='ARCHIVED'"));
            stats.put("totalPatients",   count(conn, "SELECT COUNT(*) FROM patients"));
            stats.put("totalActiveUsers",count(conn, "SELECT COUNT(*) FROM users WHERE active=TRUE"));
        }
        return stats;
    }

    private static int count(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private AdminService() {}
}
