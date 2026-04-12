package javafxapplication7;

import java.time.LocalDate;

public class PatientRecord {
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String patientNumber;
    private byte[] encryptedFile;
    private String originalFileName;

    public PatientRecord(String firstName, String lastName, LocalDate dob, String patientNumber, byte[] encryptedFile, String originalFileName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dob = dob;
        this.patientNumber = patientNumber;
        this.encryptedFile = encryptedFile;
        this.originalFileName = originalFileName;
    }

    // Getters (optional if you need them later)
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDob() { return dob; }
    public String getPatientNumber() { return patientNumber; }
    public byte[] getEncryptedFile() { return encryptedFile; }
    public String getOriginalFileName() { return originalFileName; }

    @Override
    public String toString() {
        return "PatientRecord{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", dob=" + dob +
                ", patientNumber='" + patientNumber + '\'' +
                ", fileSize=" + encryptedFile.length +
                ", originalFile='" + originalFileName + '\'' +
                '}';
    }
}
