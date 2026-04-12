package javafxapplication7.service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * All AES cryptographic operations for the system.
 *
 * Design:
 *  - Each uploaded file gets its own DEK (Data Encryption Key), generated fresh.
 *  - The DEK is wrapped (encrypted) with an application-level master key before storage.
 *  - Controllers never call this class — they go through FileService.
 *
 * Master key:
 *  Derived from a constant application secret via SHA-256.
 *  In production this should come from an encrypted keystore or HSM.
 *  To rotate: re-wrap every stored DEK with the new master key.
 */
public final class CryptoService {

    private static final String CIPHER_ALG   = "AES/CBC/PKCS5Padding";
    private static final String WRAP_ALG     = "AES/ECB/PKCS5Padding";
    private static final int    IV_SIZE      = 16;   // bytes — required for AES-CBC
    private static final int    DEK_BITS     = 256;  // AES-256

    /** Application master key (KEK). Derived once at class load. */
    private static final SecretKey MASTER_KEY = deriveMasterKey();

    private static SecretKey deriveMasterKey() {
        try {
            byte[] raw = MessageDigest.getInstance("SHA-256")
                    .digest("SecureMedicalLab::MasterKEK::2024!@#".getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(raw, "AES");
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Master KEK derivation failed: " + e.getMessage());
        }
    }

    // ── DEK + IV generation ───────────────────────────────────────────────────

    /** Generates a fresh random AES-256 DEK for one file. Never reused. */
    public static SecretKey generateDEK() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(DEK_BITS, new SecureRandom());
        return kg.generateKey();
    }

    /** Generates a fresh random 16-byte IV. Never reused. */
    public static byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    // ── Key wrapping (DEK → stored bytes) ─────────────────────────────────────

    /**
     * Wraps the DEK with the master KEK before database storage.
     * The raw DEK never touches the database.
     */
    public static byte[] wrapDEK(SecretKey dek) throws Exception {
        Cipher c = Cipher.getInstance(WRAP_ALG);
        c.init(Cipher.WRAP_MODE, MASTER_KEY);
        return c.wrap(dek);
    }

    /**
     * Recovers the DEK from the wrapped bytes read from file_keys.
     */
    public static SecretKey unwrapDEK(byte[] wrappedDEK) throws Exception {
        Cipher c = Cipher.getInstance(WRAP_ALG);
        c.init(Cipher.UNWRAP_MODE, MASTER_KEY);
        return (SecretKey) c.unwrap(wrappedDEK, "AES", Cipher.SECRET_KEY);
    }

    // ── Encrypt / Decrypt ─────────────────────────────────────────────────────

    public static byte[] encrypt(byte[] plaintext, SecretKey dek, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance(CIPHER_ALG);
        c.init(Cipher.ENCRYPT_MODE, dek, new IvParameterSpec(iv));
        return c.doFinal(plaintext);
    }

    public static byte[] decrypt(byte[] ciphertext, SecretKey dek, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance(CIPHER_ALG);
        c.init(Cipher.DECRYPT_MODE, dek, new IvParameterSpec(iv));
        return c.doFinal(ciphertext);
    }

    private CryptoService() {}
}
