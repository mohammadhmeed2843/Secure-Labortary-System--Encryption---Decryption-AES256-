package javafxapplication7;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Immutable value object that represents a single patient record
 * as used within this application's in-memory workflow.
 *
 * <p>This is NOT a database entity — it is used to carry patient data
 * between screens before it is persisted via {@link PatientFormController}.</p>
 */
public class PatientRecord {

    private final String    firstName;
    private final String    lastName;
    private final LocalDate dob;
    private final String    patientNumber;
    private final byte[]    encryptedFile;
    private final String    originalFileName;

    /**
     * Constructs a new PatientRecord.
     *
     * @param firstName        patient's first name (must not be null or blank)
     * @param lastName         patient's last name  (must not be null or blank)
     * @param dob              date of birth        (must not be null)
     * @param patientNumber    unique patient ID    (must not be null or blank)
     * @param encryptedFile    AES-encrypted PDF bytes (must not be null)
     * @param originalFileName original file name before encryption
     * @throws NullPointerException     if any required argument is null
     * @throws IllegalArgumentException if firstName, lastName, or patientNumber is blank
     */
    public PatientRecord(String firstName, String lastName, LocalDate dob,
                         String patientNumber, byte[] encryptedFile,
                         String originalFileName) {

        this.firstName        = requireNonBlank(firstName,     "firstName");
        this.lastName         = requireNonBlank(lastName,      "lastName");
        this.dob              = Objects.requireNonNull(dob,    "dob must not be null");
        this.patientNumber    = requireNonBlank(patientNumber, "patientNumber");
        this.encryptedFile    = Objects.requireNonNull(encryptedFile, "encryptedFile must not be null");
        this.originalFileName = originalFileName;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public String    getFirstName()       { return firstName;        }
    public String    getLastName()        { return lastName;         }
    public LocalDate getDob()             { return dob;              }
    public String    getPatientNumber()   { return patientNumber;    }
    public byte[]    getEncryptedFile()   { return encryptedFile;    }
    public String    getOriginalFileName(){ return originalFileName;  }

    /** Returns the patient's full name as "First Last". */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        return value.trim();
    }

    @Override
    public String toString() {
        return "PatientRecord{" +
                "name='" + getFullName() + '\'' +
                ", dob=" + dob +
                ", patientNumber='" + patientNumber + '\'' +
                ", fileSize=" + encryptedFile.length + " bytes" +
                ", originalFile='" + originalFileName + '\'' +
                '}';
    }
}
