package javafxapplication7;

/** Tiny in‑memory “vault” that lets controllers share one IV and one Key. */
public final class CryptoStore {

    private static String iv  = "";
    private static String key = "";

    public static void saveIv (String value) { iv  = value == null ? "" : value; }
    public static void saveKey(String value) { key = value == null ? "" : value; }

    public static String getIv()  { return iv;  }
    public static String getKey() { return key; }

    private CryptoStore() {}              // utility class – no instances
}
