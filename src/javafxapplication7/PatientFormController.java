package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafxapplication7.model.RecordDraft;
import javafxapplication7.service.FileService;
import javafxapplication7.service.LookupService;
import javafxapplication7.session.Session;

import java.time.LocalDate;

/**
 * Controller for the patient + test details form.
 *
 * On "Save Record":
 *   1. Validates all fields.
 *   2. Writes form values back into Session.getDraft().
 *   3. Calls FileService.uploadAndEncrypt(draft) — which encrypts, saves to DB,
 *      and securely deletes the original file atomically.
 *   4. Clears the draft and navigates to Dashboard.
 *
 * No encryption code lives here.
 */
public class PatientFormController {

    @FXML private TextField           firstNameField;
    @FXML private TextField           lastNameField;
    @FXML private DatePicker          dobPicker;
    @FXML private TextField           patientNumberField;
    @FXML private Label               fileNameLabel;

    @FXML private ComboBox<String>    testNameBox;
    @FXML private ComboBox<String>    doctorBox;
    @FXML private ComboBox<String>    technicianBox;
    @FXML private DatePicker          testDatePicker;
    @FXML private ComboBox<String>    statusBox;

    @FXML
    public void initialize() {
        // Load lookup data from database; fall back to built-ins if DB unreachable
        try {
            testNameBox.getItems().setAll(LookupService.getTestTypes());
            doctorBox.getItems().setAll(LookupService.getDoctors());
            technicianBox.getItems().setAll(LookupService.getTechnicians());
        } catch (Exception e) {
            testNameBox.getItems().addAll("Blood Test", "X-Ray", "MRI", "CT Scan", "COVID-19 PCR");
            doctorBox.getItems().addAll("Dr. Smith", "Dr. Brown", "Dr. Johnson");
            technicianBox.getItems().addAll("Tech A", "Tech B", "Tech C");
        }
        statusBox.getItems().addAll("Pending", "In Progress", "Completed");

        // Show selected file from session draft
        RecordDraft draft = Session.getDraft();
        if (fileNameLabel != null) {
            fileNameLabel.setText(draft.hasFile()
                    ? draft.getOriginalFile().getName()
                    : "(no file selected — go back and choose a PDF)");
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String    firstName     = firstNameField.getText().trim();
        String    lastName      = lastNameField.getText().trim();
        LocalDate dob           = dobPicker.getValue();
        String    patientNumber = patientNumberField.getText().trim();
        String    testName      = testNameBox.getValue();
        String    doctorName    = doctorBox.getValue();
        String    techName      = technicianBox.getValue();
        LocalDate testDate      = testDatePicker.getValue();
        String    status        = statusBox.getValue();

        if (firstName.isEmpty())     { alert(Alert.AlertType.WARNING, "First name is required.");      return; }
        if (lastName.isEmpty())      { alert(Alert.AlertType.WARNING, "Last name is required.");       return; }
        if (dob == null)             { alert(Alert.AlertType.WARNING, "Date of birth is required.");   return; }
        if (patientNumber.isEmpty()) { alert(Alert.AlertType.WARNING, "Patient number is required.");  return; }
        if (testName == null)        { alert(Alert.AlertType.WARNING, "Please select a test type.");   return; }
        if (doctorName == null)      { alert(Alert.AlertType.WARNING, "Please select a doctor.");      return; }
        if (techName == null)        { alert(Alert.AlertType.WARNING, "Please select a technician.");  return; }
        if (testDate == null)        { alert(Alert.AlertType.WARNING, "Test date is required.");       return; }
        if (status == null)          { alert(Alert.AlertType.WARNING, "Please select a status.");      return; }

        RecordDraft draft = Session.getDraft();
        if (!draft.hasFile()) {
            alert(Alert.AlertType.ERROR, "No file selected. Please go back and select a PDF.");
            return;
        }

        // Write form values into the draft
        draft.getPatient().setFirstName(firstName);
        draft.getPatient().setLastName(lastName);
        draft.getPatient().setDob(dob);
        draft.getPatient().setPatientNumber(patientNumber);
        draft.setTestType(testName);
        draft.setDoctorName(doctorName);
        draft.setTechnicianName(techName);
        draft.setTestDate(testDate);
        draft.setTestStatus(status);

        try {
            int fileId = FileService.uploadAndEncrypt(draft);
            Session.clearDraft();
            alert(Alert.AlertType.INFORMATION,
                    "Record saved successfully.\nFile ID: " + fileId +
                    "\nThe original file has been securely removed.");
            MainLayoutController.navigateToDashboard();
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Save failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        Session.clearDraft();
        MainLayoutController.navigateToUpload();
    }

    @FXML
    private void handleRandomFill(ActionEvent event) {
        firstNameField.setText("John");
        lastNameField.setText("Doe");
        dobPicker.setValue(LocalDate.of(1990, 5, 15));
        patientNumberField.setText("P" + (int)(Math.random() * 9000 + 1000));
        if (!testNameBox.getItems().isEmpty()) testNameBox.setValue(testNameBox.getItems().get(0));
        if (!doctorBox.getItems().isEmpty())   doctorBox.setValue(doctorBox.getItems().get(0));
        if (!technicianBox.getItems().isEmpty()) technicianBox.setValue(technicianBox.getItems().get(0));
        testDatePicker.setValue(LocalDate.now());
        statusBox.setValue("Pending");
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
