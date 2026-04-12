package javafxapplication7;

/**
 * In-memory vault that lets controllers share one IV, one AES Key,
 * and one encrypted file across screens without passing them through
 * constructors or FXMLLoader.
 *
 * <p>Values are held only for the lifetime of the JVM process.
 * Nothing is persisted to disk by this class.</p>
 *
 * <p>Usage pattern:</p>
 * <pre>
 *   // Home screen — after generating:
 *   CryptoStore.saveIv(generatedIv);
 *   CryptoStore.saveKey(generatedKey);
 *
 *   // Encryption screen — after encrypting:
 *   CryptoStore.saveEncryptedFile(encryptedBytes);
 *
 *   // Patient form — to read the file:
 *   byte[] file = CryptoStore.getEncryptedFile();
 * </pre>
 */
public final class CryptoStore {

    private static String iv            = "";
    private static String key           = "";
    private static byte[] encryptedFile = null;

    // ── IV ────────────────────────────────────────────────────────────────────

    /** Stores the given IV string; {@code null} is treated as empty. */
    public static void saveIv(String value)  { iv  = value == null ? "" : value; }

    /** Returns the currently stored IV, or an empty string if none was saved. */
    public static String getIv()             { return iv;  }

    // ── Key ───────────────────────────────────────────────────────────────────

    /** Stores the given AES key string; {@code null} is treated as empty. */
    public static void saveKey(String value) { key = value == null ? "" : value; }

    /** Returns the currently stored AES key, or an empty string if none was saved. */
    public static String getKey()            { return key; }

    // ── Encrypted file ────────────────────────────────────────────────────────

    /** Stores the encrypted file bytes from the current encryption session. */
    public static void saveEncryptedFile(byte[] data) { encryptedFile = data; }

    /** Returns the stored encrypted file bytes, or {@code null} if none saved. */
    public static byte[] getEncryptedFile()            { return encryptedFile; }

    /** Returns {@code true} if an encrypted file has been stored. */
    public static boolean hasEncryptedFile()           { return encryptedFile != null && encryptedFile.length > 0; }

    // ── Compound checks ───────────────────────────────────────────────────────

    /** Returns {@code true} if both IV and key have been saved and are non-empty. */
    public static boolean isReady() {
        return !iv.isEmpty() && !key.isEmpty();
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    /** Clears the IV and key but keeps the encrypted file. */
    public static void clearKeys() {
        iv  = "";
        key = "";
    }

    /** Clears all stored values: IV, key, and encrypted file. */
    public static void clearAll() {
        iv            = "";
        key           = "";
        encryptedFile = null;
    }

    private CryptoStore() {} // utility class — no instances
}
