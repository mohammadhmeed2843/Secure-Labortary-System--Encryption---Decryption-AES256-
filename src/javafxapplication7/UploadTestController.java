package javafxapplication7;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafxapplication7.model.RecordDraft;
import javafxapplication7.service.FileService;
import javafxapplication7.service.LookupService;
import javafxapplication7.service.PatientService;
import javafxapplication7.session.Session;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Unified upload screen — combines file selection + patient info + test details
 * into a single form. Encryption runs asynchronously via javafx.concurrent.Task.
 *
 * No keys, IVs, or crypto details belong here.
 */
public class UploadTestController {

    // ── Patient section ──────────────────────────────────────────────────────
    @FXML private TextField     patientNumberField;
    @FXML private TextField     firstNameField;
    @FXML private TextField     lastNameField;
    @FXML private DatePicker    dobPicker;

    // ── File section ─────────────────────────────────────────────────────────
    @FXML private TextField     filePathField;

    // ── Test details ─────────────────────────────────────────────────────────
    @FXML private ComboBox<String> testTypeBox;
    @FXML private ComboBox<String> doctorBox;
    @FXML private ComboBox<String> technicianBox;
    @FXML private DatePicker       testDatePicker;
    @FXML private ComboBox<String> statusBox;

    // ── Status / loading ─────────────────────────────────────────────────────
    @FXML private Label             statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button            saveButton;

    private File selectedFile;

    @FXML
    public void initialize() {
        try {
            testTypeBox.getItems().setAll(LookupService.getTestTypes());
            doctorBox.getItems().setAll(LookupService.getDoctors());
            technicianBox.getItems().setAll(LookupService.getTechnicians());
        } catch (Exception e) {
            testTypeBox.getItems().addAll("Blood Test", "Urinalysis", "X-Ray", "MRI", "CT Scan",
                                          "COVID-19 PCR", "Lipid Panel", "Thyroid Panel");
            doctorBox.getItems().addAll("Dr. Smith", "Dr. Brown", "Dr. Johnson", "Dr. Al-Farsi");
            technicianBox.getItems().addAll("Tech A", "Tech B", "Tech C");
        }
        statusBox.getItems().addAll("Pending", "In Progress", "Completed");
        testDatePicker.setValue(LocalDate.now());

        if (progressIndicator != null) progressIndicator.setVisible(false);
        if (statusLabel != null) statusLabel.setText("");
    }

    // ── Patient lookup ────────────────────────────────────────────────────────

    @FXML
    private void handleLookupPatient(ActionEvent e) {
        String pn = patientNumberField.getText().trim();
        if (pn.isEmpty()) { setStatus("Enter a patient number first.", true); return; }
        try {
            List<javafxapplication7.model.Patient> all = PatientService.listAll();
            all.stream()
               .filter(p -> p.getPatientNumber().equalsIgnoreCase(pn))
               .findFirst()
               .ifPresentOrElse(
                   p -> {
                       firstNameField.setText(p.getFirstName());
                       lastNameField.setText(p.getLastName());
                       if (p.getDob() != null) dobPicker.setValue(p.getDob());
                       setStatus("Patient found: " + p.getFirstName() + " " + p.getLastName(), false);
                   },
                   () -> setStatus("No patient found with number: " + pn, true)
               );
        } catch (Exception ex) {
            setStatus("Lookup failed: " + ex.getMessage(), true);
        }
    }

    // ── File picker ───────────────────────────────────────────────────────────

    @FXML
    private void handleBrowseFile(ActionEvent e) {
        FileChooser ch = new FileChooser();
        ch.setTitle("Select PDF File");
        ch.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if (desktop.exists()) ch.setInitialDirectory(desktop);
        File f = ch.showOpenDialog(stage(e));
        if (f != null) {
            selectedFile = f;
            filePathField.setText(f.getName());
            setStatus("File selected: " + f.getName(), false);
        }
    }

    // ── Save & Encrypt (async) ────────────────────────────────────────────────

    @FXML
    private void handleSave(ActionEvent e) {
        String    pn       = patientNumberField.getText().trim();
        String    fn       = firstNameField.getText().trim();
        String    ln       = lastNameField.getText().trim();
        LocalDate dob      = dobPicker.getValue();
        String    testType = testTypeBox.getValue();
        String    doctor   = doctorBox.getValue();
        String    tech     = technicianBox.getValue();
        LocalDate testDate = testDatePicker.getValue();
        String    status   = statusBox.getValue();

        // Validate all fields
        if (selectedFile == null || !selectedFile.exists()) { setStatus("Please select a PDF file.",      true); return; }
        if (pn.isEmpty())      { setStatus("Patient number is required.",  true); return; }
        if (fn.isEmpty())      { setStatus("First name is required.",      true); return; }
        if (ln.isEmpty())      { setStatus("Last name is required.",       true); return; }
        if (dob == null)       { setStatus("Date of birth is required.",   true); return; }
        if (testType == null)  { setStatus("Please select a test type.",   true); return; }
        if (doctor == null)    { setStatus("Please select a doctor.",      true); return; }
        if (tech == null)      { setStatus("Please select a technician.",  true); return; }
        if (testDate == null)  { setStatus("Test date is required.",       true); return; }
        if (status == null)    { setStatus("Please select a status.",      true); return; }

        // Build draft from form
        RecordDraft draft = Session.getDraft();
        draft.setOriginalFile(selectedFile);
        draft.getPatient().setPatientNumber(pn);
        draft.getPatient().setFirstName(fn);
        draft.getPatient().setLastName(ln);
        draft.getPatient().setDob(dob);
        draft.setTestType(testType);
        draft.setDoctorName(doctor);
        draft.setTechnicianName(tech);
        draft.setTestDate(testDate);
        draft.setTestStatus(status);

        setLoading(true);
        setStatus("Encrypting and saving — please wait…", false);

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return FileService.uploadAndEncrypt(draft);
            }
        };

        task.setOnSucceeded(ev -> {
            int fileId = task.getValue();
            Session.clearDraft();
            Platform.runLater(() -> {
                setLoading(false);
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setHeaderText(null);
                a.setContentText("Record #" + fileId + " saved and encrypted successfully.\n"
                               + "The original file has been securely removed.");
                a.showAndWait();
                MainLayoutController.navigateToDashboard();
            });
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                setLoading(false);
                setStatus("Save failed: " + (ex != null ? ex.getMessage() : "Unknown error"), true);
            });
            if (ex != null) ex.printStackTrace();
        });

        Thread t = new Thread(task, "smls-upload");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleReset(ActionEvent e) {
        Session.clearDraft();
        resetForm();
        setStatus("", false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void resetForm() {
        patientNumberField.clear();
        firstNameField.clear();
        lastNameField.clear();
        dobPicker.setValue(null);
        filePathField.clear();
        selectedFile = null;
        testTypeBox.setValue(null);
        doctorBox.setValue(null);
        technicianBox.setValue(null);
        testDatePicker.setValue(LocalDate.now());
        statusBox.setValue(null);
    }

    private void setStatus(String msg, boolean isError) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(isError
                ? "-fx-text-fill:#ef4444; -fx-font-size:12px;"
                : "-fx-text-fill:#10b981; -fx-font-size:12px;");
    }

    private void setLoading(boolean loading) {
        if (progressIndicator != null) progressIndicator.setVisible(loading);
        if (saveButton != null)        saveButton.setDisable(loading);
    }

    private static Stage stage(ActionEvent e) {
        return (Stage) ((Node) e.getSource()).getScene().getWindow();
    }
}
