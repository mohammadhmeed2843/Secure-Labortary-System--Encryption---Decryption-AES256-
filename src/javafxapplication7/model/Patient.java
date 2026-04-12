package javafxapplication7.model;

import java.time.LocalDate;

public class Patient {

    private String    patientNumber;
    private String    firstName;
    private String    lastName;
    private LocalDate dob;

    public Patient() {}

    public Patient(String patientNumber, String firstName, String lastName, LocalDate dob) {
        this.patientNumber = patientNumber;
        this.firstName     = firstName;
        this.lastName      = lastName;
        this.dob           = dob;
    }

    public String    getPatientNumber()                        { return patientNumber; }
    public void      setPatientNumber(String patientNumber)    { this.patientNumber = patientNumber; }
    public String    getFirstName()                            { return firstName; }
    public void      setFirstName(String firstName)            { this.firstName = firstName; }
    public String    getLastName()                             { return lastName; }
    public void      setLastName(String lastName)              { this.lastName = lastName; }
    public LocalDate getDob()                                  { return dob; }
    public void      setDob(LocalDate dob)                     { this.dob = dob; }

    public String getFullName() { return firstName + " " + lastName; }

    @Override
    public String toString() { return getFullName() + " (" + patientNumber + ")"; }
}
