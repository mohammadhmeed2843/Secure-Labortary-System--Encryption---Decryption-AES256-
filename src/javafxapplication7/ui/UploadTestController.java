package javafxapplication7.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafxapplication7.models.FileRecord;
import javafxapplication7.models.Patient;
import javafxapplication7.models.RecordDraft;
import javafxapplication7.services.FileService;
import javafxapplication7.services.LookupService;
import javafxapplication7.services.PatientService;
import javafxapplication7.services.PermissionService;
import javafxapplication7.session.Session;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class UploadTestController {

    @FXML private TextField            patientNumberField;
    @FXML private TextField            firstNameField;
    @FXML private TextField            lastNameField;
    @FXML private DatePicker           dobPicker;
    @FXML private TextField            filePathField;
    @FXML private ComboBox<String>     testTypeBox;
    @FXML private ComboBox<String>     doctorBox;
    @FXML private ComboBox<String>     technicianBox;
    @FXML private DatePicker           testDatePicker;
    @FXML private ComboBox<String>     statusBox;
    @FXML private VBox                 existingRecordsSection;
    @FXML private ComboBox<FileRecord> supersededRecordBox;
    @FXML private Label                statusLabel;
    @FXML private ProgressIndicator    progressIndicator;
    @FXML private Button               saveButton;

    private File selectedFile;

    @FXML
    public void initialize() {
        if (!Session.isLoggedIn() ||
                !PermissionService.canUpload(Session.getUser().getRole())) {
            if (statusLabel != null)
                statusLabel.setText("Access denied: insufficient permissions.");
            if (saveButton != null) saveButton.setDisable(true);
            return;
        }

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
        if (progressIndicator     != null) progressIndicator.setVisible(false);
        if (statusLabel           != null) statusLabel.setText("");
        if (existingRecordsSection != null) existingRecordsSection.setVisible(false);
    }

    @FXML
    private void handleLookupPatient(ActionEvent e) {
        String pn = patientNumberField.getText().trim();
        if (pn.isEmpty()) { setStatus("Enter a patient number first.", true); return; }
        try {
            List<Patient> all = PatientService.listAll();
            all.stream()
               .filter(p -> p.getPatientNumber().equalsIgnoreCase(pn))
               .findFirst()
               .ifPresentOrElse(
                   p -> {
                       firstNameField.setText(p.getFirstName());
                       lastNameField.setText(p.getLastName());
                       if (p.getDob() != null) dobPicker.setValue(p.getDob());
                       setStatus("Patient found: " + p.getFirstName() + " " + p.getLastName(), false);
                       loadExistingRecords(pn);
                   },
                   () -> {
                       setStatus("No patient found — a new patient record will be created.", false);
                       hideExistingRecords();
                   }
               );
        } catch (Exception ex) {
            setStatus("Lookup failed: " + ex.getMessage(), true);
        }
    }

    private void loadExistingRecords(String patientNumber) {
        if (existingRecordsSection == null) return;
        try {
            List<FileRecord> active = FileService.listForPatient(patientNumber).stream()
                    .filter(r -> !"ARCHIVED".equals(r.getStatus()))
                    .toList();
            if (active.isEmpty()) { hideExistingRecords(); return; }
            supersededRecordBox.getItems().setAll(active);
            supersededRecordBox.setPromptText("— New record (no supersede) —");
            supersededRecordBox.setValue(null);
            existingRecordsSection.setVisible(true);
            existingRecordsSection.setManaged(true);
        } catch (Exception e) {
            hideExistingRecords();
        }
    }

    private void hideExistingRecords() {
        if (existingRecordsSection != null) {
            existingRecordsSection.setVisible(false);
            existingRecordsSection.setManaged(false);
        }
        if (supersededRecordBox != null) supersededRecordBox.setValue(null);
    }

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

        if (selectedFile == null || !selectedFile.exists()) { setStatus("Please select a PDF file.", true); return; }
        if (pn.isEmpty())     { setStatus("Patient number is required.", true); return; }
        if (fn.isEmpty())     { setStatus("First name is required.",     true); return; }
        if (ln.isEmpty())     { setStatus("Last name is required.",      true); return; }
        if (dob == null)      { setStatus("Date of birth is required.",  true); return; }
        if (testType == null) { setStatus("Please select a test type.",  true); return; }
        if (doctor == null)   { setStatus("Please select a doctor.",     true); return; }
        if (tech == null)     { setStatus("Please select a technician.", true); return; }
        if (testDate == null) { setStatus("Test date is required.",      true); return; }
        if (status == null)   { setStatus("Please select a status.",     true); return; }

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

        FileRecord supersedes = (supersededRecordBox != null) ? supersededRecordBox.getValue() : null;
        final int previousFileId = (supersedes != null) ? supersedes.getFileId() : -1;

        setLoading(true);
        setStatus((previousFileId > 0) ? "Saving new version…" : "Encrypting and saving…", false);

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() throws Exception {
                return (previousFileId > 0)
                    ? FileService.uploadNewVersion(draft, previousFileId)
                    : FileService.uploadAndEncrypt(draft);
            }
        };

        task.setOnSucceeded(ev -> {
            int fileId = task.getValue();
            Session.clearDraft();
            Platform.runLater(() -> {
                setLoading(false);
                String msg = (previousFileId > 0)
                    ? "Version updated. New record #" + fileId + " saved.\nPrevious record archived."
                    : "Record #" + fileId + " saved and encrypted.\nOriginal file securely removed.";
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setHeaderText(null);
                a.setContentText(msg);
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
        patientNumberField.clear(); firstNameField.clear(); lastNameField.clear();
        dobPicker.setValue(null); filePathField.clear(); selectedFile = null;
        testTypeBox.setValue(null); doctorBox.setValue(null); technicianBox.setValue(null);
        testDatePicker.setValue(LocalDate.now()); statusBox.setValue(null);
        hideExistingRecords();
        setStatus("", false);
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
