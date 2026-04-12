package javafxapplication7;

/**
 * In-memory vault that lets controllers share one IV and one AES Key
 * across screens without passing them through constructors.
 *
 * <p>Values are held only for the lifetime of the JVM process.
 * Neither the IV nor the key is persisted to disk by this class.</p>
 *
 * <p>Usage pattern:</p>
 * <pre>
 *   // On the Home screen — after generating:
 *   CryptoStore.saveIv(generatedIv);
 *   CryptoStore.saveKey(generatedKey);
 *
 *   // On the Encryption / Decryption screen — to retrieve:
 *   String iv  = CryptoStore.getIv();
 *   String key = CryptoStore.getKey();
 * </pre>
 */
public final class CryptoStore {

    private static String iv  = "";
    private static String key = "";

    /** Stores the given IV string; {@code null} is treated as empty. */
    public static void saveIv(String value)  { iv  = value == null ? "" : value; }

    /** Stores the given AES key string; {@code null} is treated as empty. */
    public static void saveKey(String value) { key = value == null ? "" : value; }

    /** Returns the currently stored IV, or an empty string if none was saved. */
    public static String getIv()  { return iv;  }

    /** Returns the currently stored AES key, or an empty string if none was saved. */
    public static String getKey() { return key; }

    /** Returns {@code true} if both IV and key have been saved and are non-empty. */
    public static boolean isReady() {
        return !iv.isEmpty() && !key.isEmpty();
    }

    /** Clears both the IV and the key from memory. */
    public static void clear() {
        iv  = "";
        key = "";
    }

    private CryptoStore() {} // utility class — no instances
}
