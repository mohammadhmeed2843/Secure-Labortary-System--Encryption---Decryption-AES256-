package javafxapplication7;

import java.sql.Connection;

public class EncryptionDB  {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.connect();
            System.out.println("✅ Connected to PostgreSQL!");
            conn.close();
        } catch (Exception e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
        }
    }
}