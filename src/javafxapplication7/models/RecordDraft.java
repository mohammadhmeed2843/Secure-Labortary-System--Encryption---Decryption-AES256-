package javafxapplication7.models;

import java.io.File;
import java.time.LocalDate;

/**
 * Transient in-memory accumulator for the "new record" workflow.
 * A fresh draft is created when the user starts a new upload.
 * FileService reads this object to perform the atomic encrypt+save operation.
 */
public class RecordDraft {

    private Patient   patient      = new Patient();
    private File      originalFile;
    private String    testType;
    private String    doctorName;
    private String    technicianName;
    private LocalDate testDate;
    private String    testStatus   = "Pending";

    public Patient   getPatient()                           { return patient;          }
    public void      setPatient(Patient p)                  { this.patient = p;        }
    public File      getOriginalFile()                      { return originalFile;     }
    public void      setOriginalFile(File f)                { this.originalFile = f;   }
    public String    getTestType()                          { return testType;         }
    public void      setTestType(String v)                  { this.testType = v;       }
    public String    getDoctorName()                        { return doctorName;       }
    public void      setDoctorName(String v)                { this.doctorName = v;     }
    public String    getTechnicianName()                    { return technicianName;   }
    public void      setTechnicianName(String v)            { this.technicianName = v; }
    public LocalDate getTestDate()                          { return testDate;         }
    public void      setTestDate(LocalDate v)               { this.testDate = v;       }
    public String    getTestStatus()                        { return testStatus;       }
    public void      setTestStatus(String v)                { this.testStatus = v;     }

    public boolean hasFile() {
        return originalFile != null && originalFile.exists();
    }

    public boolean hasPatient() {
        Patient p = patient;
        return p.getPatientNumber() != null && !p.getPatientNumber().isBlank()
            && p.getFirstName()     != null && !p.getFirstName().isBlank()
            && p.getLastName()      != null && !p.getLastName().isBlank()
            && p.getDob()           != null;
    }
}
