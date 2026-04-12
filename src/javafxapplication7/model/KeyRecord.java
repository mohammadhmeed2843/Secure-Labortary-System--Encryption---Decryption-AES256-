package javafxapplication7.model;

/**
 * Represents one row from the file_keys table.
 * encryptedKey is the per-file AES-256 DEK wrapped with the application master key.
 * iv is the 16-byte random CBC initialization vector.
 */
public class KeyRecord {

    private final int    fileId;
    private final byte[] encryptedKey;
    private final byte[] iv;

    public KeyRecord(int fileId, byte[] encryptedKey, byte[] iv) {
        this.fileId       = fileId;
        this.encryptedKey = encryptedKey;
        this.iv           = iv;
    }

    public int    getFileId()       { return fileId;       }
    public byte[] getEncryptedKey() { return encryptedKey; }
    public byte[] getIv()           { return iv;           }
}
