package javafxapplication7.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a row from medical_files joined with related lookup tables.
 * encryptedData is only populated when loading a single record for export;
 * list queries leave it null to avoid loading large blobs unnecessarily.
 */
public class FileRecord {

    private int           fileId;
    private String        patientNumber;
    private String        patientName;       // first_name || ' ' || last_name
    private byte[]        encryptedData;     // null in list views
    private String        originalName;
    private String        testType;
    private String        doctorName;
    private String        technicianName;
    private LocalDate     testDate;
    private String        testStatus;
    private String        status;            // READY | VIEWED | ARCHIVED
    private int           uploadedBy;
    private String        uploaderName;
    private LocalDateTime uploadedAt;

    public FileRecord() {}

    public int           getFileId()         { return fileId;         }
    public void          setFileId(int v)    { this.fileId = v;       }
    public String        getPatientNumber()  { return patientNumber;  }
    public void          setPatientNumber(String v) { this.patientNumber = v; }
    public String        getPatientName()    { return patientName;    }
    public void          setPatientName(String v) { this.patientName = v; }
    public byte[]        getEncryptedData()  { return encryptedData;  }
    public void          setEncryptedData(byte[] v) { this.encryptedData = v; }
    public String        getOriginalName()   { return originalName;   }
    public void          setOriginalName(String v) { this.originalName = v; }
    public String        getTestType()       { return testType;       }
    public void          setTestType(String v) { this.testType = v;   }
    public String        getDoctorName()     { return doctorName;     }
    public void          setDoctorName(String v) { this.doctorName = v; }
    public String        getTechnicianName() { return technicianName; }
    public void          setTechnicianName(String v) { this.technicianName = v; }
    public LocalDate     getTestDate()       { return testDate;       }
    public void          setTestDate(LocalDate v) { this.testDate = v; }
    public String        getTestStatus()     { return testStatus;     }
    public void          setTestStatus(String v) { this.testStatus = v; }
    public String        getStatus()         { return status;         }
    public void          setStatus(String v) { this.status = v;       }
    public int           getUploadedBy()     { return uploadedBy;     }
    public void          setUploadedBy(int v) { this.uploadedBy = v; }
    public String        getUploaderName()   { return uploaderName;   }
    public void          setUploaderName(String v) { this.uploaderName = v; }
    public LocalDateTime getUploadedAt()     { return uploadedAt;     }
    public void          setUploadedAt(LocalDateTime v) { this.uploadedAt = v; }

    /** Human-readable summary for display in combo-boxes and lists. */
    public String toDisplayString() {
        String date = testDate != null ? testDate.toString() : "no date";
        String type = testType != null ? testType : "Unknown test";
        String name = patientName != null ? patientName : patientNumber;
        return "#" + fileId + " — " + name + " · " + type + " · " + date;
    }

    @Override public String toString() { return toDisplayString(); }
}
