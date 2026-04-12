package javafxapplication7.service;

import javafxapplication7.DatabaseConnection;
import javafxapplication7.model.KeyRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Data-access layer for the file_keys table.
 * Every method that writes is called inside an existing transaction (accepts Connection).
 * Read methods open their own connection.
 */
public final class KeyService {

    /**
     * Inserts one row into file_keys within an existing open transaction.
     * wrappedDEK is the per-file AES key already encrypted with the master KEK.
     */
    public static void save(Connection conn, int fileId, byte[] wrappedDEK, byte[] iv) throws Exception {
        String sql = "INSERT INTO file_keys (file_id, encrypted_key, iv) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ps.setBytes(2, wrappedDEK);
            ps.setBytes(3, iv);
            ps.executeUpdate();
        }
    }

    /**
     * Loads the KeyRecord for a given file_id.
     * Throws if no key exists (this indicates a corrupted record).
     */
    public static KeyRecord load(int fileId) throws Exception {
        String sql = "SELECT encrypted_key, iv FROM file_keys WHERE file_id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new KeyRecord(fileId, rs.getBytes("encrypted_key"), rs.getBytes("iv"));
            }
        }
        throw new Exception("No encryption key found for file_id=" + fileId
                + ". The record may be corrupted.");
    }

    private KeyService() {}
}
