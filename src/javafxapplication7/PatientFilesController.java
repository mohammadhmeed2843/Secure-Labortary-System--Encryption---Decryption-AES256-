package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafxapplication7.model.FileRecord;
import javafxapplication7.model.Patient;
import javafxapplication7.service.FileService;
import javafxapplication7.service.PatientService;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Doctor-facing screen: search patients on the left, view their encrypted
 * lab files on the right. One click on "View File" auto-decrypts and opens
 * the PDF in the default system viewer.
 *
 * No keys or crypto details are visible anywhere on this screen.
 */
public class PatientFilesController {

    @FXML private TextField searchPatientField;
    @FXML private VBox      patientListContainer;
    @FXML private Label     selectedPatientLabel;
    @FXML private VBox      filesContainer;
    @FXML private Label     filesHeaderLabel;

    private List<Patient> allPatients = new ArrayList<>();
    private Patient       activePatient;
    private Button        activePatientBtn;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @FXML
    public void initialize() {
        searchPatientField.textProperty().addListener((obs, o, n) -> filterPatients(n));
        loadPatients();
    }

    @FXML
    private void handleRefreshPatients(ActionEvent e) {
        loadPatients();
        clearFilePanel();
    }

    private void loadPatients() {
        try {
            allPatients = PatientService.listAll();
            renderPatientList(allPatients);
        } catch (Exception ex) {
            ex.printStackTrace();
            renderError(patientListContainer, "Could not load patients: " + ex.getMessage());
        }
    }

    private void filterPatients(String query) {
        if (query == null || query.isBlank()) {
            renderPatientList(allPatients);
            return;
        }
        String q = query.toLowerCase();
        renderPatientList(allPatients.stream()
            .filter(p -> (p.getFirstName() + " " + p.getLastName() + " " + p.getPatientNumber())
                          .toLowerCase().contains(q))
            .toList());
    }

    private void renderPatientList(List<Patient> patients) {
        patientListContainer.getChildren().clear();
        if (patients.isEmpty()) {
            Label lbl = new Label("No patients found.");
            lbl.setStyle("-fx-text-fill:#3d4f65; -fx-font-size:12px; -fx-padding:16 16;");
            patientListContainer.getChildren().add(lbl);
            return;
        }
        for (Patient p : patients) {
            Button btn = new Button(p.getFirstName() + " " + p.getLastName() +
                                    "\n" + p.getPatientNumber());
            btn.getStyleClass().add("patient-list-item");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setOnAction(e -> selectPatient(p, btn));
            if (activePatient != null && activePatient.getPatientNumber().equals(p.getPatientNumber())) {
                btn.getStyleClass().add("patient-list-item-active");
                activePatientBtn = btn;
            }
            patientListContainer.getChildren().add(btn);
        }
    }

    private void selectPatient(Patient p, Button btn) {
        // Deactivate previous
        if (activePatientBtn != null) {
            activePatientBtn.getStyleClass().remove("patient-list-item-active");
        }
        btn.getStyleClass().add("patient-list-item-active");
        activePatient    = p;
        activePatientBtn = btn;

        selectedPatientLabel.setText(p.getFirstName() + " " + p.getLastName()
                                     + "  ·  " + p.getPatientNumber());
        loadFilesForPatient(p.getPatientNumber());
    }

    private void loadFilesForPatient(String patientNumber) {
        filesContainer.getChildren().clear();
        filesHeaderLabel.setText("Loading…");
        try {
            List<FileRecord> records = FileService.listForPatient(patientNumber).stream()
                .filter(r -> "READY".equals(r.getStatus()) || "VIEWED".equals(r.getStatus()))
                .toList();

            if (records.isEmpty()) {
                filesHeaderLabel.setText("No files available");
                Label lbl = new Label("No lab reports are available for this patient.");
                lbl.setStyle("-fx-text-fill:#3d4f65; -fx-font-size:13px; -fx-padding:24 0;");
                filesContainer.getChildren().add(lbl);
                return;
            }

            filesHeaderLabel.setText(records.size() + " file" + (records.size() == 1 ? "" : "s") + " available");
            for (FileRecord r : records) {
                filesContainer.getChildren().add(buildFileCard(r));
            }
        } catch (Exception ex) {
            filesHeaderLabel.setText("Error");
            renderError(filesContainer, "Could not load files: " + ex.getMessage());
        }
    }

    private VBox buildFileCard(FileRecord r) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label testName = new Label(nullStr(r.getTestType(), "Lab Report"));
        testName.setStyle("-fx-text-fill:#e8eaf0; -fx-font-size:14px; -fx-font-weight:bold;");

        Label date = new Label(r.getTestDate() != null ? r.getTestDate().format(DATE_FMT) : "Date unknown");
        date.setStyle("-fx-text-fill:#5a6480; -fx-font-size:11px;");

        Label doctor = new Label("Ordered by: " + nullStr(r.getDoctorName(), "—"));
        doctor.setStyle("-fx-text-fill:#5a6480; -fx-font-size:11px;");

        info.getChildren().addAll(testName, date, doctor);

        Label badge = new Label(nullStr(r.getStatus(), "READY"));
        badge.getStyleClass().add(badgeClass(r.getStatus()));

        Button viewBtn = new Button("View File");
        viewBtn.getStyleClass().add("btn-primary");
        viewBtn.setStyle("-fx-font-size:12px; -fx-padding:8 18;");
        viewBtn.setOnAction(e -> handleViewFile(r.getFileId(), viewBtn));

        topRow.getChildren().addAll(info, badge, viewBtn);
        card.getChildren().add(topRow);

        if (r.getOriginalName() != null && !r.getOriginalName().isBlank()) {
            Label fname = new Label(r.getOriginalName());
            fname.setStyle("-fx-text-fill:#3d4f65; -fx-font-size:11px;");
            card.getChildren().add(fname);
        }

        return card;
    }

    private void handleViewFile(int fileId, Button btn) {
        btn.setDisable(true);
        btn.setText("Opening…");

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "smls_view");

        Thread t = new Thread(() -> {
            try {
                File pdf = FileService.exportDecrypted(fileId, tempDir);
                pdf.deleteOnExit(); // clean up temp decrypted file on exit

                javafx.application.Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("View File");
                    try {
                        java.awt.Desktop.getDesktop().open(pdf);
                    } catch (Exception ex) {
                        showError("Could not open PDF viewer: " + ex.getMessage());
                    }
                    // Refresh to show VIEWED badge
                    if (activePatient != null) {
                        loadFilesForPatient(activePatient.getPatientNumber());
                    }
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("View File");
                    showError("Failed to open file: " + ex.getMessage());
                });
            }
        }, "smls-view");
        t.setDaemon(true);
        t.start();
    }

    private void clearFilePanel() {
        activePatient = null;
        activePatientBtn = null;
        selectedPatientLabel.setText("Select a patient");
        filesHeaderLabel.setText("No patient selected");
        filesContainer.getChildren().clear();
    }

    private static String badgeClass(String status) {
        if (status == null) return "badge-ready";
        return switch (status.toUpperCase()) {
            case "READY"    -> "badge-ready";
            case "VIEWED"   -> "badge-viewed";
            case "ARCHIVED" -> "badge-archived";
            default         -> "badge-pending";
        };
    }

    private static String nullStr(String s, String def) {
        return (s != null && !s.isBlank()) ? s : def;
    }

    private static void renderError(VBox container, String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill:#ef4444; -fx-font-size:12px; -fx-padding:8 0;");
        container.getChildren().add(lbl);
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
